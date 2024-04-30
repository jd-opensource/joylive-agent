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
package com.jd.live.agent.bootstrap.bytekit.context;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * An abstract class representing an executable context.
 * This class provides a structure to hold information related to an executable task or operation.
 */
public abstract class ExecutableContext {

    /**
     * The type of the executable.
     */
    @Getter
    protected final Class<?> type;

    /**
     * The arguments passed to the executable.
     */
    @Getter
    protected final Object[] arguments;

    /**
     * A description of the executable context.
     */
    @Getter
    protected final String description;

    /**
     * The target object of the executable context.
     */
    @Setter
    @Getter
    protected Object target;

    /**
     * Any throwable that occurred during the execution of the executable.
     */
    @Setter
    @Getter
    protected Throwable throwable;

    /**
     * A map of attributes associated with the executable context.
     */
    protected Map<String, Object> attributes;

    /**
     * Creates a new instance of ExecutableContext.
     *
     * @param type        the type of the executable
     * @param arguments   the arguments passed to the executable
     * @param description a description of the executable context
     */
    public ExecutableContext(Class<?> type, Object[] arguments, String description) {
        this.type = type;
        this.arguments = arguments;
        this.description = description;
    }


    /**
     * Checks if the execution should be skipped.
     *
     * @return {@code true} if the execution should be skipped, {@code false} otherwise
     */
    public boolean isSkip() {
        return false;
    }

    /**
     * Checks if the execution was successful, i.e. no throwable occurred.
     *
     * @return {@code true} if the execution was successful, {@code false} otherwise
     */
    public boolean isSuccess() {
        return throwable == null;
    }

    /**
     * Adds an attribute to the executable context.
     *
     * @param key the key for the attribute
     * @param obj the value of the attribute
     * @throws NullPointerException if the key is {@code null} or empty
     */
    public void addAttribute(String key, Object obj) {
        if (key != null && !key.isEmpty()) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key, obj);
        }
    }

    /**
     * Retrieves an attribute from the executable context.
     *
     * @param key the key for the attribute
     * @param <T> the type of the attribute
     * @return the value of the attribute, or {@code null} if the key is {@code null} or empty, or the attribute is not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return key == null || key.isEmpty() ? null : (T) attributes.get(key);
    }

}
