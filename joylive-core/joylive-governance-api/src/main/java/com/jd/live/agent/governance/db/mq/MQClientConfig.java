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
package com.jd.live.agent.governance.db.mq;

/**
 * Basic configuration interface for message queue clients.
 */
public interface MQClientConfig {

    /**
     * Gets the message queue server address.
     */
    String getServerAddress();

    /**
     * Sets the message queue server address.
     *
     * @param address the server connection address
     */
    void setServerAddress(String address);

    /**
     * Gets the client type identifier.
     */
    String getType();
}