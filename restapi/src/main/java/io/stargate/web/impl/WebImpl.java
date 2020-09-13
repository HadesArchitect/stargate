package io.stargate.web.impl;

import io.stargate.auth.AuthenticationService;
import io.stargate.db.Persistence;
import io.stargate.health.metrics.api.Metrics;

public class WebImpl {

    private Persistence persistence;
    private AuthenticationService authenticationService;
    private Metrics metrics;

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public void start() throws Exception {
        Server server = new Server(persistence, this.authenticationService, this.metrics);
        server.run("server", "config.yaml");
    }
}
