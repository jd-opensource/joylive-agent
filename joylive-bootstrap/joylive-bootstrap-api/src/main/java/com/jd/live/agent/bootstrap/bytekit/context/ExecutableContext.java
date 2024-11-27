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

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * An abstract class representing an executable context.
 * This class provides a structure to hold information related to an executable task or operation.
 */
@Getter
public abstract class ExecutableContext extends AbstractAttributes {

    protected static ThreadLocal<Boolean> INVOKE_ORIGIN_METHOD;

    private static final AtomicLong COUNTER = new AtomicLong(0);

    /**
     * The id of the executable
     */
    protected final long id;

    /**
     * The type of the executable.
     */
    protected final Class<?> type;

    /**
     * The arguments passed to the executable.
     */
    protected final Object[] arguments;

    /**
     * A description of the executable context.
     */
    protected final String description;

    /**
     * The target object of the executable context.
     */
    @Setter
    protected Object target;

    /**
     * Any throwable that occurred during the execution of the executable.
     */
    @Setter
    protected Throwable throwable;

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
        this.id = COUNTER.incrementAndGet();
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

    @SuppressWarnings("unchecked")
    public <T> T getArgument(int index) {
        return arguments == null || index < 0 || index >= arguments.length ? null : (T) arguments[index];
    }

    /**
     * Marks the origin of the current invocation.
     */
    protected void markOrigin() {
        if (INVOKE_ORIGIN_METHOD == null) {
            INVOKE_ORIGIN_METHOD = new ThreadLocal<>();
        }
        INVOKE_ORIGIN_METHOD.set(Boolean.TRUE);
    }

    /**
     * Gets and removes the origin flag for the current thread.
     *
     * @return true if the current thread has marked the origin of the invocation, false otherwise.
     */
    public boolean getAndRemoveOrigin() {
        if (INVOKE_ORIGIN_METHOD == null) {
            return false;
        } else {
            Boolean result = INVOKE_ORIGIN_METHOD.get();
            INVOKE_ORIGIN_METHOD.set(Boolean.FALSE);
            return result != null && result;
        }
    }

    /**
     * Gets the origin flag for the current thread.
     *
     * @return true if the current thread has marked the origin of the invocation, false otherwise.
     */
    public boolean isOrigin() {
        if (INVOKE_ORIGIN_METHOD == null) {
            return false;
        } else {
            Boolean result = INVOKE_ORIGIN_METHOD.get();
            return result != null && result;
        }
    }

}
