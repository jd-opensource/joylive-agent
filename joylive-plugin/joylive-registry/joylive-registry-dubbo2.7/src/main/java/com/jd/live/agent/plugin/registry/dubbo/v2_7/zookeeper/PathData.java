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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper;

import com.jd.live.agent.core.util.network.Ipv4;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * Immutable value object representing a ZooKeeper node path and its associated data.
 */
@Getter
public class PathData {

    public static final byte[] DEFAULT_DATA = getDefaultData();

    private final String path;

    private final byte[] data;

    private final boolean persistent;

    public PathData(String path) {
        this(path, DEFAULT_DATA, false);
    }

    public PathData(String path, boolean persistent) {
        this(path, DEFAULT_DATA, persistent);
    }

    public PathData(String path, byte[] data) {
        this(path, data == null ? DEFAULT_DATA : data, false);
    }

    public PathData(String path, String data) {
        this(path, data, false);
    }

    public PathData(String path, String data, boolean persistent) {
        this(path, data == null || data.isEmpty() ? DEFAULT_DATA : data.getBytes(StandardCharsets.UTF_8), persistent);
    }

    public PathData(String path, byte[] data, boolean persistent) {
        this.path = path;
        this.data = data == null ? DEFAULT_DATA : data;
        this.persistent = persistent;
    }

    private static byte[] getDefaultData() {
        String ip = Ipv4.getLocalIp();
        return ip == null ? new byte[0] : ip.getBytes(StandardCharsets.UTF_8);
    }
}
