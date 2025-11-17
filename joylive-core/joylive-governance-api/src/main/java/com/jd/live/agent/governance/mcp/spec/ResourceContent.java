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

/**
 * A common interface for resource content, which includes metadata about the resource
 * such as its URI, name, description, MIME type, size, and annotations. This
 * interface is implemented by both {@link Resource} and {@link ResourceLink} to
 * provide a consistent way to access resource metadata.
 */
public interface ResourceContent extends Identifier, Annotated, Meta {

    String getUri();

    String getDescription();

    String getMimeType();

    Long getSize();

}
