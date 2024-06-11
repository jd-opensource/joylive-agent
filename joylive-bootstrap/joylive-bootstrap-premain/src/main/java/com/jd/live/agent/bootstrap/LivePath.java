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
package com.jd.live.agent.bootstrap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Map;

public abstract class LivePath {

    public static final String KEY_AGENT_PATH = "LIVE_AGENT_ROOT";

    public static final String ARG_AGENT_PATH = "agentPath";

    public static final String DIR_LIB = "lib";

    public static final String DIR_LIB_SYSTEM = "system";

    public static final String DIR_LIB_CORE = "core";

    public static final String DIR_CONFIG = "config";

    public static final String DIR_PLUGIN = "plugin";

    public static final String JAR_FILE_PREFIX = "jar:file:";

    public static final String LIVE_AGENT_PATH = "LiveAgent.path";

    public static final String LIVE_JAR = "live.jar";

    /**
     * Determines the root path of the agent based on the provided arguments and environment.
     *
     * @param env  A map containing the environment configuration.
     * @param args A map containing the agent's arguments.
     * @return A file representing the root path of the agent.
     */
    public static File getRootPath(Map<String, ?> env, Map<String, Object> args) {
        File result = null;
        String root = args == null ? null : (String) args.get(LivePath.ARG_AGENT_PATH);
        if (root == null || root.isEmpty()) {
            root = (String) env.get(LivePath.KEY_AGENT_PATH);
            if (root == null || root.isEmpty()) {
                ProtectionDomain protectionDomain = LiveAgent.class.getProtectionDomain();
                CodeSource codeSource = protectionDomain == null ? null : protectionDomain.getCodeSource();
                if (codeSource != null) {
                    String path = urlDecode(codeSource.getLocation().getPath());
                    result = new File(path).getParentFile();
                } else {
                    URL url = ClassLoader.getSystemClassLoader().getResource(LivePath.LIVE_AGENT_PATH);
                    if (url != null) {
                        String path = url.toString();
                        if (path.startsWith(LivePath.JAR_FILE_PREFIX)) {
                            int pos = path.lastIndexOf('/');
                            int end = path.lastIndexOf('/', pos - 1);
                            result = new File(urlDecode(path.substring(LivePath.JAR_FILE_PREFIX.length(), end)));
                        }
                    }
                }
            } else {
                result = new File(root);
            }
        } else {
            result = new File(root);
        }
        return result;
    }

    /**
     * Decodes a URL encoded string using UTF-8 encoding.
     *
     * @param value The string to be decoded.
     * @return The decoded string.
     */
    private static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Returns the original value if UTF-8 encoding is not supported.
            return value;
        }
    }

}
