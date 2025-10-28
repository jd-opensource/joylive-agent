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

/**
 * Interface for parsing system parameters with support for optional values.
 */
@FunctionalInterface
public interface RequestParser {

    /**
     * Parses parameter from context.
     *
     * @param ctx parser context
     * @return parsed value
     * @throws Exception if parsing fails
     */
    Object parse(RequestContext ctx) throws Exception;

    /**
     * Combines multiple request parsers and executes them in sequence until a non-null result is found.
     */
    class CompositeRequestParser implements RequestParser {

        private RequestParser[] parsers;

        public CompositeRequestParser(RequestParser... parsers) {
            this.parsers = parsers;
        }

        @Override
        public Object parse(RequestContext ctx) throws Exception {
            Object result = null;
            if (parsers != null) {
                for (RequestParser parser : parsers) {
                    result = parser.parse(ctx);
                    if (result != null) {
                        break;
                    }
                }
            }
            return result;
        }
    }

}
