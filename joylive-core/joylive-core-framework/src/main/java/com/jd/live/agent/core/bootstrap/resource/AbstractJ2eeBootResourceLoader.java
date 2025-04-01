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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.jd.live.agent.core.bootstrap.resource.BootResource.SCHEMA_CLASSPATH;
import static com.jd.live.agent.core.util.StringUtils.concat;

/**
 * A class that implements the ResourceFinder interface by searching for resources in a web container.
 */
public abstract class AbstractJ2eeBootResourceLoader implements BootResourceLoader {

    @Override
    public InputStreamResource getResource(BootResource resource) throws IOException {
        File workingDirectory = new File(System.getProperty("user.dir"));
        if (workingDirectory.getName().equals("bin")) {
            workingDirectory = workingDirectory.getParentFile();
        }
        String webAppDirectory = getWebAppDirectory();
        File webappDirectory = new File(workingDirectory, webAppDirectory);
        if (!webappDirectory.exists()) {
            String home = System.getProperty(getWebHomePropertyName());
            if (home != null) {
                webappDirectory = new File(home, webAppDirectory);
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
                        InputStreamResource inputStream = getInputStream(file, path, webappDirectory);
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
     * Gets the web application's root directory path.
     * <p>
     * Implementations should return the absolute filesystem path where the web application
     * resources are located. This typically corresponds to the web application's deployment
     * directory (e.g., "webapps" directory in Tomcat).
     *
     * @return the absolute path to the web application directory (never {@code null})
     * @throws IllegalStateException if the directory cannot be determined
     */
    protected abstract String getWebAppDirectory();

    /**
     * Gets the system property name used to locate the web server home directory.
     * <p>
     * Implementations should return the name of the system property that contains
     * the installation directory of the web server/container (e.g., "catalina.home"
     * for Tomcat).
     *
     * @return the system property name (never {@code null})
     */
    protected abstract String getWebHomePropertyName();

    /**
     * A private helper method that tries to find the resource in a given file.
     *
     * @param file            The file to search in.
     * @param name            The name of the resource to find.
     * @param webappDirectory The web application directory.
     * @return The input stream of the resource, or null if the resource could not be found.
     * @throws IOException If an I/O error occurs while trying to read the resource.
     */
    private InputStreamResource getInputStream(File file, String name, File webappDirectory) throws IOException {
        if (file.isDirectory()) {
            File resourceFile = new File(file, name);
            return !resourceFile.exists() ? null : new InputStreamResource(Files.newInputStream(resourceFile.toPath()), resourceFile.getPath());
        }
        return null;
    }
}
