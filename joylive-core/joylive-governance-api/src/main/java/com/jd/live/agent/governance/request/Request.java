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
package com.jd.live.agent.governance.request;

/**
 * Represents a general request interface.
 * <p>
 * This interface defines a common structure for requests, including constants and methods that might be necessary for
 * handling requests. It serves as a base for more specific request types.
 * </p>
 *
 * @since 1.0.0
 */
public interface Request {

    /**
     * A constant key used for identifying sticky sessions in live services.
     * <p>
     * This key can be used in request headers or parameters to maintain session stickiness across multiple requests.
     * </p>
     */
    String KEY_STICKY_ID = "x-live-sticky-id";

}

