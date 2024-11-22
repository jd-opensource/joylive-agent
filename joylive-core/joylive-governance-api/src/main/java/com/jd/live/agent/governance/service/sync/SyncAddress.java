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
package com.jd.live.agent.governance.service.sync;

public interface SyncAddress {

    /**
     * An interface that defines the configuration for synchronizing spaces with an HTTP service.
     */
    interface LaneSpaceAddress {

        /**
         * Returns the URL for lane spaces.
         *
         * @return The URL for lane spaces.
         */
        String getLaneSpacesUrl();

        /**
         * Returns the URL for an individual lane space.
         *
         * @return The URL for an individual lane space.
         */
        String getLaneSpaceUrl();
    }

    /**
     * An interface that defines the configuration for synchronizing spaces with an HTTP service.
     */
    interface LiveSpaceAddress {

        /**
         * Returns the URL for spaces.
         *
         * @return The URL for spaces.
         */
        String getLiveSpacesUrl();

        /**
         * Returns the URL for an individual space.
         *
         * @return The URL for an individual space.
         */
        String getLiveSpaceUrl();
    }

    /**
     * An interface that defines the configuration for an HTTP service.
     */
    interface ServiceAddress {

        /**
         * Returns the URL of the HTTP service.
         *
         * @return The URL of the HTTP service.
         */
        String getServiceUrl();
    }
}


