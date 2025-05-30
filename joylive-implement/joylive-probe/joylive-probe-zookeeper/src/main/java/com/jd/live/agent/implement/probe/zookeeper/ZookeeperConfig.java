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
package com.jd.live.agent.implement.probe.zookeeper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZookeeperConfig {

    public static final String COMMAND_SRVR = "srvr";

    private int connectTimeout = 1000;

    private String command = COMMAND_SRVR;

    private String response;

}
