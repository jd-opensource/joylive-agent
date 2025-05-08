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
package com.jd.live.agent.governance.invoke.auth;

import lombok.Getter;

/**
 * Represents the result of a rate limit acquisition attempt.
 * Contains success status and optional message for failures.
 */
@Getter
public class Permission {

    private final boolean success;

    private final int errorCode;

    private final String message;

    public Permission(boolean success, int errorCode, String message) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * Successful acquisition result
     *
     * @return successful acquisition result with no message
     */
    public static Permission success() {
        return new Permission(true, 0, null);
    }

    /**
     * Failed acquisition result
     *
     * @param message description of the failure reason
     * @return failed acquisition result with message
     */
    public static Permission failure(String message) {
        return new Permission(false, 0, message);
    }

    /**
     * Failed acquisition result
     *
     * @param errorCode error code
     * @param message   description of the failure reason
     * @return failed acquisition result with message
     */
    public static Permission failure(int errorCode, String message) {
        return new Permission(false, errorCode, message);
    }
}
