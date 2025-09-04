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
package com.jd.live.agent.governance.util;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class FrameworkVersion implements Serializable {

    public static final String DUBBO = "dubbo";
    public static final String SPRING_CLOUD = "spring-cloud";
    public static final String SPRING_BOOT = "spring-boot";
    public static final String SOFA_RPC = "sofa-rpc";

    private final String framework;

    private final String version;

    public FrameworkVersion(String framework, String version) {
        this.framework = framework;
        this.version = version;
    }

    public FrameworkVersion(String framework, Class<?> type, String defaultVersion) {
        this.framework = framework;
        String ver = type == null ? null : type.getPackage().getImplementationVersion();
        this.version = ver == null || ver.isEmpty() ? defaultVersion : ver;
    }

    @Override
    public String toString() {
        if (version == null || version.isEmpty()) {
            return framework;
        }
        return framework + "-" + version;
    }

    public static FrameworkVersion dubbo(String version) {
        return new FrameworkVersion(DUBBO, version);
    }

    public static FrameworkVersion springCloud(String version) {
        return new FrameworkVersion(SPRING_CLOUD, version);
    }

    public static FrameworkVersion springBoot(String version) {
        return new FrameworkVersion(SPRING_BOOT, version);
    }

    public static FrameworkVersion sofaRpc(String version) {
        return new FrameworkVersion(SOFA_RPC, version);
    }

}
