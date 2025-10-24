/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.mcp;

import java.util.function.Supplier;

/**
 * Interface for parsing system parameters with support for optional values.
 */
public interface ParameterParser {

    /**
     * Parses and returns the parameter value.
     *
     * @return the parsed parameter value
     */
    Object parse();

    /**
     * Indicates if the parameter is optional.
     *
     * @return true if parameter is optional, false otherwise
     */
    boolean isOptional();

    /**
     * Checks if the value can be converted to the target type.
     *
     * @return true if conversion is possible, false otherwise
     */
    boolean isConvertable();

    /**
     * Default implementation of SystemParameterParser.
     */
    class DefaultParameterParser implements ParameterParser {

        private final boolean optional;
        private final boolean convertable;
        private final Supplier<Object> supplier;

        public DefaultParameterParser(Supplier<Object> supplier) {
            this(false, false, supplier);
        }

        public DefaultParameterParser(boolean optional, boolean convertable, Supplier<Object> supplier) {
            this.optional = optional;
            this.convertable = convertable;
            this.supplier = supplier;
        }

        @Override
        public Object parse() {
            return supplier.get();
        }

        @Override
        public boolean isOptional() {
            return optional;
        }

        @Override
        public boolean isConvertable() {
            return convertable;
        }
    }
}
