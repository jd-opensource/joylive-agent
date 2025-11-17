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

import java.io.Serializable;

/**
 * The contents of a specific resource or sub-resource.
 */
public interface ResourceContents extends Serializable {

    /**
     * The URI of this resource.
     *
     * @return the URI of this resource.
     */
    String getUri();

    /**
     * The MIME type of this resource.
     *
     * @return the MIME type of this resource.
     */
    String getMimeType();
}
