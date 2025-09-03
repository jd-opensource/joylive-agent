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
package com.jd.live.agent.core.bootstrap.resource;

import lombok.Getter;

@Getter
public class BootResource {

    public static final String SCHEMA_CLASSPATH = "classpath";

    public static final String SCHEMA_FILE = "file";

    private final String schema;

    private final String path;

    private final String name;

    private final String extension;

    public BootResource(String schema, String path, String name) {
        this.schema = schema;
        this.path = path;
        this.name = name;
        int pos = name.lastIndexOf(".");
        this.extension = pos > 0 ? name.substring(pos + 1) : "";
    }

    public boolean withPath() {
        return path != null && !path.isEmpty();
    }

    public BootResource profile(String profile) {
        String n = !extension.isEmpty() ? name.substring(0, name.length() - extension.length() - 1) : name;
        return new BootResource(schema, path, n + "-" + profile + "." + extension);
    }
}
