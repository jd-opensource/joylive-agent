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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.core.util.URI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class McpConfig {

    public static final int DEFAULT_CHECK_INTERVAL = 10000;
    public static final int DEFAULT_TIME_OUT = 60 * 1000;

    private boolean enabled;

    private String title;

    private String version;

    private String path = "/mcp";

    private long checkInterval = DEFAULT_CHECK_INTERVAL;

    private long timeout = DEFAULT_TIME_OUT;

    private boolean governanceEnabled = true;

    public boolean isMcpPath(String path) {
        return enabled && URI.isSubPath(path, this.path);
    }

    protected void initialize() {
        if (path == null || path.isEmpty()) {
            path = "/mcp";
        } else {
            if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
                path = path.substring(0, path.length() - 1);
            }
            if (path.length() > 0 && path.charAt(0) != '/') {
                path = "/" + path;
            }
        }
    }

    public long getCheckInterval() {
        if (checkInterval <= 0) {
            return DEFAULT_CHECK_INTERVAL;
        }
        return checkInterval;
    }

    public long getTimeout() {
        if (timeout <= 0) {
            return DEFAULT_TIME_OUT;
        }
        return timeout;
    }
}

