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
package com.jd.live.agent.implement.parser.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract base class for Jackson-based JSON processing.
 * Provides pre-configured ObjectMapper with custom settings.
 */
public abstract class AbstractJackson {

    /**
     * The ObjectMapper instance used for JSON parsing and serialization.
     */
    protected ObjectMapper mapper;

    /**
     * Constructs an AbstractJacksonParser and initializes the ObjectMapper with custom configuration.
     */
    public AbstractJackson() {
        mapper = configure(new ObjectMapper(createFactory())).registerModules(
                ObjectMapper.findModules(AbstractJackson.class.getClassLoader()));
    }

    /**
     * Creates a JsonFactory instance. Subclasses can override this method to provide a custom JsonFactory.
     *
     * @return a new JsonFactory instance.
     */
    protected JsonFactory createFactory() {
        return null;
    }

    /**
     * Configures the given ObjectMapper with custom settings.
     *
     * @param mapper the ObjectMapper to configure.
     * @return the configured ObjectMapper.
     */
    @SuppressWarnings("deprecation")
    protected ObjectMapper configure(ObjectMapper mapper) {
        return mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setAnnotationIntrospector(new JsonAnnotationIntrospector());
    }
}
