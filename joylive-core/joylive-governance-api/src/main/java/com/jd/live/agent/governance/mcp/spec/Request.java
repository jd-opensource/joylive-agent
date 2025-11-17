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
package com.jd.live.agent.governance.mcp.spec;

import java.util.Map;

/**
 * Represents a request with metadata capabilities.
 */
public interface Request {

    String MCP_PROGRESS_TOKEN = "progressToken";

    String KEY_VERSION = "x-mcp-version";

    interface MetaRequest extends Request, Meta {

        /**
         * Retrieves the progress token from metadata.
         *
         * @return the progress token if available, null otherwise
         */
        default Object progressToken() {
            Map<String, Object> meta = getMeta();
            if (meta != null && meta.containsKey(MCP_PROGRESS_TOKEN)) {
                return meta.get(MCP_PROGRESS_TOKEN).toString();
            }
            return null;
        }
    }
}
