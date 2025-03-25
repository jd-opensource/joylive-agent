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
package com.jd.live.agent.core.bootstrap.resource.tomcat;

import com.jd.live.agent.core.bootstrap.resource.BootResource;
import com.jd.live.agent.core.bootstrap.resource.BootResourceLoader;
import com.jd.live.agent.core.extension.annotation.Extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.jd.live.agent.core.bootstrap.resource.BootResource.SCHEMA_CLASSPATH;
import static com.jd.live.agent.core.util.StringUtils.concat;

/**
 * A class that implements the ResourceFinder interface by searching for resources in a Tomcat web application.
 */
@Extension(value = "TomcatBootResourceLoader", order = BootResourceLoader.ORDER_TOMCAT)
public class TomcatBootResourceLoader implements BootResourceLoader {

    @Override
    public InputStream getResource(BootResource resource) throws IOException {
        File workingDirectory = new File(System.getProperty("user.dir"));
        if (workingDirectory.getName().equals("bin")) {
            workingDirectory = workingDirectory.getParentFile();
        }
        File webappDirectory = new File(workingDirectory, "webapps/");
        if (!webappDirectory.exists()) {
            String home = System.getProperty("catalina.home");
            if (home != null) {
                webappDirectory = new File(home, "webapps/");
            }
        }
        if (webappDirectory.exists()) {
            String[] paths = resource.withPath()
                    ? new String[]{concat(resource.getPath(), resource.getPath(), "/")}
                    : new String[]{"WEB-INF/classes/" + resource.getName(), "WEB-INF/classes/config/" + resource.getName()};
            File[] files = webappDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    for (String path : paths) {
                        InputStream inputStream = getInputStream(file, path, webappDirectory);
                        if (inputStream != null) {
                            return inputStream;
                        }
                    }

                }
            }
        }
        return null;
    }

    @Override
    public boolean support(String schema) {
        return schema == null || schema.isEmpty() || SCHEMA_CLASSPATH.equals(schema);
    }

    /**
     * A private helper method that tries to find the resource in a given file.
     *
     * @param file            The file to search in.
     * @param name            The name of the resource to find.
     * @param webappDirectory The web application directory.
     * @return The input stream of the resource, or null if the resource could not be found.
     * @throws IOException If an I/O error occurs while trying to read the resource.
     */
    private InputStream getInputStream(File file, String name, File webappDirectory) throws IOException {
        if (file.isFile() && file.getName().endsWith(".war")) {
            return getInputStream(file, name);
        } else if (file.isDirectory()) {
            File resourceFile = new File(file, name);
            return !resourceFile.exists() ? null : Files.newInputStream(resourceFile.toPath());
        }
        return null;
    }

    /**
     * A private helper method that tries to find the resource in a given WAR file.
     *
     * @param file The WAR file to search in.
     * @param name The name of the resource to find.
     * @return The input stream of the resource, or null if the resource could not be found.
     * @throws IOException If an I/O error occurs while trying to read the resource.
     */
    private InputStream getInputStream(File file, String name) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().equals(name)) {
                    return jarFile.getInputStream(entry);
                }
            }
        }
        return null;
    }
}
