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
package com.jd.live.agent.core.bootstrap.resource.file;

import com.jd.live.agent.core.bootstrap.resource.BootResource;
import com.jd.live.agent.core.bootstrap.resource.BootResourceLoader;
import com.jd.live.agent.core.extension.annotation.Extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.jd.live.agent.core.bootstrap.resource.BootResource.SCHEMA_FILE;
import static com.jd.live.agent.core.util.StringUtils.concat;

/**
 * A class that implements the ResourceFinder interface by searching for resources on the file.
 */
@Extension(value = "FileBootResourceLoader", order = BootResourceLoader.ORDER_FILE)
public class FileBootResourceLoader implements BootResourceLoader {

    @Override
    public InputStream getResource(BootResource resource) throws IOException {
        String[] paths = resource.withPath()
                ? new String[]{concat(resource.getPath(), resource.getName(), File.pathSeparator)}
                : new String[]{resource.getName(), concat("config", resource.getName(), File.pathSeparator)};
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                return Files.newInputStream(file.toPath());
            }
        }
        return null;
    }

    @Override
    public boolean support(String schema) {
        return schema == null || schema.isEmpty() || SCHEMA_FILE.equalsIgnoreCase(schema);
    }
}
