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

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * A known resource that the server is capable of reading.
 */
@Getter
@Setter
public class Resource implements ResourceContent, Icons {

    /**
     * the URI of the resource.
     */
    private String uri;
    /**
     * A human-readable name for this resource. This can be used by clients to
     * populate UI elements.
     */
    private String name;
    /**
     * An optional title for this resource.
     */
    private String title;
    /**
     * A description of what this resource represents. This can be used
     * by clients to improve the LLM's understanding of available resources. It can be
     * thought of like a "hint" to the model.
     */
    private String description;
    /**
     * The MIME type of this resource, if known.
     */
    private String mimeType;
    /**
     * The size of the raw resource content, in bytes (i.e., before base64
     * encoding or any tokenization), if known. This can be used by Hosts to display file
     * sizes and estimate context window usage.
     */
    private Long size;
    /**
     * Optional annotations for the client. The client can use
     * annotations to inform how objects are used or displayed.
     */
    private Annotations annotations;

    /**
     * Optional set of sized icons that the client can display in a user interface.
     */
    private List<Icon> icons;

    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public Resource() {
    }

    public Resource(String uri, String name, String title, String description, String mimeType, Long size, Annotations annotations, List<Icon> icons, Map<String, Object> meta) {
        this.uri = uri;
        this.name = name;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
        this.size = size;
        this.annotations = annotations;
        this.icons = icons;
        this.meta = meta;
    }

    /**
     * A manual builder for {@link Resource} that supports inheritance.
     * This builder can be extended by subclasses to provide type-safe builders for Resource subclasses.
     *
     * @param <T> The type of Resource being built
     * @param <B> The type of Builder (self-type for method chaining)
     */
    public abstract static class AbstractResourceBuilder<T extends Resource, B extends AbstractResourceBuilder<T, B>> {

        protected String uri;
        protected String name;
        protected String title;
        protected String description;
        protected String mimeType;
        protected Long size;
        protected Annotations annotations;
        protected List<Icon> icons;
        protected Map<String, Object> meta;

        /**
         * Returns this builder for method chaining.
         *
         * @return this builder
         */
        protected abstract B self();

        /**
         * Creates a new instance of the Resource being built.
         *
         * @return a new Resource instance
         */
        protected abstract T createInstance();

        public B uri(String uri) {
            this.uri = uri;
            return self();
        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B title(String title) {
            this.title = title;
            return self();
        }

        public B description(String description) {
            this.description = description;
            return self();
        }

        public B mimeType(String mimeType) {
            this.mimeType = mimeType;
            return self();
        }

        public B size(Long size) {
            this.size = size;
            return self();
        }

        public B annotations(Annotations annotations) {
            this.annotations = annotations;
            return self();
        }

        public B icons(List<Icon> icons) {
            this.icons = icons;
            return self();
        }

        public B meta(Map<String, Object> meta) {
            this.meta = meta;
            return self();
        }

        /**
         * Builds a new Resource instance with the current builder values.
         *
         * @return a new Resource instance
         */
        public T build() {
            T instance = createInstance();
            instance.setUri(uri);
            instance.setName(name);
            instance.setTitle(title);
            instance.setDescription(description);
            instance.setMimeType(mimeType);
            instance.setSize(size);
            instance.setAnnotations(annotations);
            instance.setIcons(icons);
            instance.setMeta(meta);
            return instance;
        }
    }

    /**
     * A concrete implementation of ResourceBuilder for the Resource class.
     */
    public static class ResourceBuilder extends AbstractResourceBuilder<Resource, ResourceBuilder> {
        @Override
        protected ResourceBuilder self() {
            return this;
        }

        @Override
        protected Resource createInstance() {
            return new Resource();
        }

        /**
         * Creates a new builder for Resource.
         *
         * @return a new ResourceBuilder
         */
        public static ResourceBuilder builder() {
            return new ResourceBuilder();
        }
    }
}
