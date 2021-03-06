/*
 * Copyright DataStax, Inc. and/or The Stargate Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.stargate.db.datastore.query;

import java.util.Optional;
import java.util.function.Function;

import io.stargate.db.datastore.schema.Column;

public interface Parameter<T> extends Comparable<Parameter>
{
    Column column();

    Optional<Object> value();

    Object UNSET = new SpecialTermMarker()
    {
        final static String term = "<unset>";
        @Override
        public String toString()
        {
            return term;
        }

        @Override
        public int hashCode()
        {
            return term.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null && this.toString().equals(obj.toString());
        }
    };

    Object NULL = new SpecialTermMarker()
    {
        static final String term = "<null>";

        @Override
        public String toString()
        {
            return term;
        }

        @Override
        public int hashCode()
        {
            return term.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null && this.toString().equals(obj.toString());
        }
    };

    Optional<Function<T, Object>> bindingFunction();

    default int compareTo(Parameter other)
    {
        return column().name().compareTo(other.column().name());
    }

    default boolean ignored()
    {
        return false;
    }

    default Parameter<T> ignore()
    {
        return this;
    }

    interface SpecialTermMarker{}
}
