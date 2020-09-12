package io.stargate.cql.impl;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import io.stargate.health.metrics.api.Metrics;
import org.apache.cassandra.stargate.metrics.ClientMetrics;
import org.apache.cassandra.stargate.transport.internal.Server;
import org.apache.cassandra.stargate.transport.internal.TransportDescriptor;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.utils.NativeLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.stargate.db.Persistence;

public class CqlImpl {
    private static final Logger logger = LoggerFactory.getLogger(CqlImpl.class);

    private volatile Persistence<?, ?, ?> persistence;
    private volatile Metrics metrics;
    private Collection<Server> servers = Collections.emptyList();
    final private EventLoopGroup workerGroup;

    public CqlImpl(Config config) {
        TransportDescriptor.daemonInitialization(config);

        if (useEpoll())
        {
            workerGroup = new EpollEventLoopGroup();
            logger.info("Netty using native Epoll event loop");
        }
        else
        {
            workerGroup = new NioEventLoopGroup();
            logger.info("Netty using Java NIO event loop");
        }

    }

    public Persistence<?, ?, ?> getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence<?, ?, ?> persistence) {
        this.persistence = persistence;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public void start() {
        int nativePort = TransportDescriptor.getNativeTransportPort();
        int nativePortSSL = TransportDescriptor.getNativeTransportPortSSL();
        InetAddress nativeAddr = TransportDescriptor.getRpcAddress();

        Server.Builder builder = new Server.Builder(persistence)
                .withEventLoopGroup(workerGroup)
                .withHost(nativeAddr);

        if (!TransportDescriptor.getNativeProtocolEncryptionOptions().enabled)
        {
            servers = Collections.singleton(builder.withSSL(false).withPort(nativePort).build());
        }
        else
        {
            if (nativePort != nativePortSSL)
            {
                // user asked for dedicated ssl port for supporting both non-ssl and ssl connections
                servers = Collections.unmodifiableList(
                        Arrays.asList(
                                builder.withSSL(false).withPort(nativePort).build(),
                                builder.withSSL(true).withPort(nativePortSSL).build()
                        )
                );
            }
            else
            {
                // ssl only mode using configured native port
                servers = Collections.singleton(builder.withSSL(true).withPort(nativePort).build());
            }
        }

        ClientMetrics.instance.init(servers, metrics.getRegistry());
        servers.forEach(Server::start);
    }

    public static boolean useEpoll()
    {
        final boolean enableEpoll = Boolean.parseBoolean(System.getProperty("stargate.cql.native.epoll.enabled", "true"));

        if (enableEpoll && !Epoll.isAvailable() && NativeLibrary.osType == NativeLibrary.OSType.LINUX)
            logger.warn("epoll not available", Epoll.unavailabilityCause());

        return enableEpoll && Epoll.isAvailable();
    }
}
