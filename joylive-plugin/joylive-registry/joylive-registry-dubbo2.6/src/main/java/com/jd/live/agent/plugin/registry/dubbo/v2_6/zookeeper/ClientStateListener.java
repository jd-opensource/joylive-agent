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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.zookeeper;

import com.alibaba.dubbo.remoting.zookeeper.StateListener;

public interface ClientStateListener extends StateListener {

    int SESSION_LOST = 0;

    int CONNECTED = 1;

    int RECONNECTED = 2;

    int SUSPENDED = 3;

    int NEW_SESSION_CREATED = 4;
}
