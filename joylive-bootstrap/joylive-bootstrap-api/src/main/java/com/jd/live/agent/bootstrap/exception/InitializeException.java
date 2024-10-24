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
package com.jd.live.agent.bootstrap.exception;

/**
 * Represents exceptions that can occur during initialization processes.
 * <p>This exception is designed not to fill in the stack trace for performance reasons,
 * as stack trace generation is an expensive operation and might not be necessary for
 * all initialization failures.</p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class InitializeException extends LiveException {
    public InitializeException() {
    }

    public InitializeException(String message) {
        super(message, null, false, false);
    }

    public InitializeException(String message, Throwable cause) {
        super(message, cause, false, false);
    }
}
