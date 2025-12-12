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

import java.util.List;
import java.util.Map;

/**
 * A known resource that the server is capable of reading.
 */
public class ResourceLink extends Resource implements Content {

    private String type = TYPE_RESOURCE_LINK;

    public ResourceLink() {
    }

    public ResourceLink(String uri, String name, String title, String description, String mimeType, Long size) {
        super(uri, name, title, description, mimeType, size, null, null, null);
    }

    public ResourceLink(String uri,
                        String name,
                        String title,
                        String description,
                        String mimeType,
                        Long size,
                        Annotations annotations,
                        List<Icon> icons,
                        Map<String, Object> meta) {
        super(uri, name, title, description, mimeType, size, annotations, icons, meta);
    }

    @Override
    public String getType() {
        return TYPE_RESOURCE_LINK;
    }

    public void setType(String type) {

    }

    /**
     * ResourceLink's Builder implementation
     */
    public static class ResourceLinkBuilder extends AbstractResourceBuilder<ResourceLink, ResourceLinkBuilder> {

        @Override
        protected ResourceLinkBuilder self() {
            return this;
        }

        @Override
        protected ResourceLink createInstance() {
            return new ResourceLink();
        }

        /**
         * Creates a new builder for ResourceLink.
         *
         * @return a new ResourceLinkBuilder
         */
        public static ResourceLinkBuilder builder() {
            return new ResourceLinkBuilder();
        }
    }

}
