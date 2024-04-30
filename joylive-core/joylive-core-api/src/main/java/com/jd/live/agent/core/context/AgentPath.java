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
package com.jd.live.agent.core.context;

import com.jd.live.agent.bootstrap.exception.DirectoryException;
import lombok.Getter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * AgentPath
 *
 * @since 1.0.0
 */
@Getter
public class AgentPath {

    public static final String KEY_AGENT_PATH = "LIVE_AGENT_ROOT";

    public static final String KEY_AGENT_LOG_PATH = "LIVE_AGENT_LOG_PATH";

    public static final String KEY_AGENT_OUTPUT_PATH = "LIVE_AGENT_OUTPUT_PATH";

    public static final String DIR_CONFIG = "config";

    public static final String DIR_PLUGIN = "plugin";

    public static final String DIR_OUTPUT = "output";

    public static final String DIR_LOG = "log";

    public static final String DIR_LIB = "lib";

    public static final String DIR_LIB_SYSTEM = "system";

    public static final String DIR_LIB_CORE = "core";

    public static final String DIR_LIB_CORE_IMPL = "core.impl";

    public static final String DIR_LIB_COMMON = "common";

    public static final String FILE_CONFIG = "config.yaml";

    public static final String FILE_BOOTSTRAP = "bootstrap.properties";

    public static final String COMPONENT_AGENT_PATH = "agentPath";

    private final File root;

    private final File pluginPath;

    private final File outputPath;

    private final File logPath;

    private final File libPath;

    private final File systemLibPath;

    private final File coreLibPath;

    private final File coreImplLibPath;

    private final File configPath;

    private final File configFile;

    public AgentPath(File root) {
        this(root, null, null);
    }

    public AgentPath(File root, File logPath, File outputPath) {
        this.root = root;
        this.pluginPath = createDir(root, DIR_PLUGIN, false, true);
        this.outputPath = outputPath != null ? createDir(outputPath, true, true) :
                createDir(root, DIR_OUTPUT, true, true);
        this.logPath = logPath != null ? createDir(logPath, true, false) :
                createDir(root, DIR_LOG, true, false);
        this.libPath = createDir(root, DIR_LIB, false, true);
        this.systemLibPath = createDir(libPath, DIR_LIB_SYSTEM, false, true);
        this.coreLibPath = createDir(libPath, DIR_LIB_CORE, false, true);
        this.coreImplLibPath = createDir(libPath, DIR_LIB_CORE_IMPL, false, true);
        this.configPath = createDir(root, DIR_CONFIG, false, true);
        this.configFile = createFile(configPath, FILE_CONFIG, false);
    }

    protected File createDir(File root, String path, boolean optional, boolean readOnly) {
        return createDir(new File(root, path + "/"), optional, readOnly);
    }

    protected File createDir(File file, boolean optional, boolean readOnly) {
        if (!file.exists()) {
            if (!optional)
                throw new DirectoryException("directory is not exists. " + file.getPath());
            else if (!file.mkdirs()) {
                return null;
            }
        }
        if (!file.canRead())
            throw new DirectoryException("directory is not readable. " + file.getPath());
        if (!readOnly && !file.canWrite())
            throw new DirectoryException("directory is not writeable. " + file.getPath());
        return file;
    }

    protected File createFile(File root, String path, boolean optional) {
        File file = new File(root, path);
        if (!file.exists()) {
            if (!optional)
                throw new DirectoryException("file is not exists. " + file.getPath());
        } else if (!file.canRead()) {
            throw new DirectoryException("file is not readable. " + file.getPath());
        }
        return file;
    }

    public File[] getLibs(File path) {
        if (path == null)
            return new File[0];
        return path.listFiles(pathname -> pathname.isFile() && pathname.canRead() && pathname.getName().endsWith(".jar"));
    }

    public URL[] getLibUrls(File path) {
        File[] files = getLibs(path);
        URL[] result = new URL[files.length];
        for (int i = 0; i < result.length; i++) {
            try {
                result[i] = files[i].toURI().toURL();
            } catch (MalformedURLException e) {
                throw new DirectoryException("malformed url " + files[i].getPath(), e);
            }
        }
        return result;
    }

    public Map<String, File> getPlugins() {
        Map<String, File> result = new HashMap<>();
        File[] files = pluginPath.listFiles(pathname -> pathname.isDirectory() && pathname.canRead());
        if (files != null)
            for (File file : files) {
                result.put(file.getName(), file);
            }
        return result;
    }
}
