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
package com.jd.live.agent.implement.service.config.nacos.client;

import com.jd.live.agent.governance.config.ConfigCenterConfig;
import lombok.Getter;

@Getter
public class NacosProperties {

    private final String url;

    private final String username;

    private final String password;

    private final String namespace;

    private final long timeout;

    private final boolean grayEnabled;

    public NacosProperties(String url, String username, String password, String namespace, long timeout, boolean grayEnabled) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.namespace = namespace;
        this.timeout = timeout;
        this.grayEnabled = grayEnabled;
    }

    public NacosProperties(ConfigCenterConfig config, String namespace) {
        this(config.getAddress(), config.getUsername(), config.getPassword(), namespace, config.getTimeout(), config.isGrayEnabled());
    }
}
