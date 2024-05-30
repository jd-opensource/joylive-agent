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
package com.jd.live.agent.governance.policy.service.live;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Represents a remote configuration detail for a unit or cell within a distributed system.
 */
@Getter
@Setter
public class RemoteCnd implements Serializable {

    /**
     * The name of the remote configuration.
     */
    private String name;

    /**
     * The type of the remote configuration. The default type is {@link RemoteType#INSTANCES}, suggesting that the default
     * configuration pertains to instances of services.
     */
    private RemoteType type = RemoteType.INSTANCES;

    /**
     * A threshold value associated with this remote configuration.
     */
    private int threshold;

    @Override
    public RemoteCnd clone() {
        try {
            return (RemoteCnd) super.clone();
        } catch (CloneNotSupportedException ignore) {
            return null;
        }
    }
}

