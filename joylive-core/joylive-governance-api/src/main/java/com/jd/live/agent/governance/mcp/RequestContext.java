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

import com.jd.live.agent.core.parser.ObjectConverter;
import lombok.Getter;

/**
 * Context interface for MCP request parameter conversion.
 *
 * <p>Provides access to an ObjectConverter that transforms MCP request
 * parameters into method argument types.
 *
 * @see ObjectConverter
 */
public interface RequestContext {

    /**
     * Gets the converter for transforming request parameters.
     *
     * @return the object converter instance
     */
    ObjectConverter getConverter();

    @Getter
    abstract class AbstractRequestContext implements RequestContext {

        private ObjectConverter converter;

        public AbstractRequestContext(ObjectConverter converter) {
            this.converter = converter;
        }

    }
}
