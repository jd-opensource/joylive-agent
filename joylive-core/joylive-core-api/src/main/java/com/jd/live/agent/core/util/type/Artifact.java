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
package com.jd.live.agent.core.util.type;

import lombok.Getter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.lang.Character.isDigit;

/***
 * Artifact represents Maven artifact information and is capable of extracting it from a JAR file.
 * It holds the groupId, artifactId, and version of the artifact.
 */
@Getter
public class Artifact {

    public static final String VERSION = "version";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String GROUP_ID = "groupId";
    public static final String IMPLEMENTATION_VERSION = "Implementation-Version";
    public static final String POM_PROPERTIES = "pom.properties";

    // Group ID of the Maven artifact.
    private String groupId;

    // Artifact ID of the Maven artifact.
    private String artifactId;

    // Version of the Maven artifact.
    private String version;

    /**
     * Constructs an Artifact object by extracting the artifact information from the specified JAR file path.
     *
     * @param path The file path to the JAR file from which to extract artifact information.
     */
    public Artifact(String path) {
        if (path != null && !path.isEmpty()) {
            try (JarFile jarFile = new JarFile(path)) {
                // get artifactId and version from file name.
                Map<String, String> fileInfo = getFileInfo(new File(path));
                artifactId = fileInfo.get(ARTIFACT_ID);
                version = fileInfo.get(VERSION);
                // get groupId,artifactId and version from pom.properties
                Properties mavenInfo = getMavenInfo(jarFile);
                if (mavenInfo != null) {
                    version = mavenInfo.getProperty(VERSION);
                    groupId = mavenInfo.getProperty(GROUP_ID);
                } else if (version == null || version.isEmpty()) {
                    Map<String, String> manifestInfo = getManifestInfo(jarFile);
                    version = manifestInfo.get(IMPLEMENTATION_VERSION);
                }
            } catch (IOException ignored) {
            }
        }

    }

    /**
     * Extracts manifest information from the given JAR file.
     *
     * @param jarFile The JAR file from which to extract manifest information.
     * @return A map containing the manifest information with the manifest keys and their corresponding values.
     * @throws IOException if an I/O error occurs while reading the manifest.
     */
    private Map<String, String> getManifestInfo(JarFile jarFile) throws IOException {
        /**
         * Manifest-Version: 1.0
         * Created-By: Apache Ant 1.5.1
         * Extension-Name: Struts Framework
         * Specification-Title: Struts Framework
         * Specification-Vendor: Apache Software Foundation
         * Specification-Version: 1.1
         * Implementation-Title: Struts Framework
         * Implementation-Vendor: Apache Software Foundation
         * Implementation-Vendor-Id: org.apache
         * Implementation-Version: 1.1
         * Class-Path:  commons-beanutils.jar commons-collections.jar commons-dig
         *  ester.jar commons-logging.jar commons-validator.jar jakarta-oro.jar s
         *  truts-legacy.jar
         */
        Map<String, String> result = new HashMap<>();
        Manifest manifest = jarFile.getManifest();
        for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }

    /**
     * Extracts Maven information from the given JAR file.
     *
     * @param jarFile The JAR file from which to extract Maven information.
     * @return A Properties object containing Maven information such as groupId, artifactId, and version.
     * @throws IOException if an I/O error occurs while reading the pom.properties file.
     */
    private Properties getMavenInfo(JarFile jarFile) throws IOException {
        Enumeration<JarEntry> enumeration = jarFile.entries();
        JarEntry entry;
        String path = artifactId == null ? POM_PROPERTIES : artifactId + "/" + POM_PROPERTIES;
        while (enumeration.hasMoreElements()) {
            entry = enumeration.nextElement();
            if (entry.getName().endsWith(path)) {
                try (InputStream inputStream = new BufferedInputStream(jarFile.getInputStream(entry))) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    return properties;
                }
            }
        }
        return null;
    }

    /**
     * Extracts file information from the given file, specifically the artifactId and version.
     *
     * @param file The file from which to extract the information.
     * @return A map containing the artifactId and version extracted from the file name.
     */
    private Map<String, String> getFileInfo(File file) {
        Map<String, String> result = new HashMap<>();
        String name = file.getName();
        // Remove extension
        int pos = name.lastIndexOf('.');
        name = name.substring(0, pos);

        int start = name.indexOf('-');
        while (start > 0) {
            if (isDigit(name.charAt(start + 1))) {
                result.put(VERSION, name.substring(start + 1));
                result.put(ARTIFACT_ID, name.substring(0, start));
                break;
            }
            start = name.indexOf('-', start + 1);
        }
        return result;
    }

    /**
     * Gets version from the JAR file containing the specified class.
     *
     * @param type The class to get version for
     * @return Version string from JAR manifest
     */
    public static String getVersion(Class<?> type) {
        if (type == null) {
            return null;
        }
        CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        Artifact artifact = new Artifact(codeSource.getLocation().getPath());
        return artifact.getVersion();
    }

}
