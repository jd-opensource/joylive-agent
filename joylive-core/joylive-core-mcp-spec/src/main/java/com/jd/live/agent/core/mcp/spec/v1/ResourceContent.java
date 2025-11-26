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
package com.jd.live.agent.core.mcp.spec.v1;

/**
 * A common interface for resource content, which includes metadata about the resource
 * such as its URI, name, description, MIME type, size, and annotations. This
 * interface is implemented by both {@link Resource} and {@link ResourceLink} to
 * provide a consistent way to access resource metadata.
 */
public interface ResourceContent extends Identifier, Annotated, Meta {

    /**
     * the URI of the resource.
     */
    String getUri();

    /**
     * A description of what this resource represents. This can be used
     * by clients to improve the LLM's understanding of available resources. It can be
     * thought of like a "hint" to the model.
     */
    String getDescription();

    /**
     * The MIME type of this resource.
     *
     * @return the MIME type of this resource.
     */
    String getMimeType();

    /**
     * The size of the raw resource content, in bytes (i.e., before base64
     * encoding or any tokenization), if known. This can be used by Hosts to display file
     * sizes and estimate context window usage.
     */
    Long getSize();

}
