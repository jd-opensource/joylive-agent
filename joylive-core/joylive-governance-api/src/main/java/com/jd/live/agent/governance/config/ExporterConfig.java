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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.core.inject.annotation.Config;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ExporterConfig {

    public static final String CONFIG_EXPORTER = "agent.exporter";

    private boolean enabled = true;

    /**
     * Exporter type, currently supported: logging, otlp.grpc, otlp.http
     */
    private String type = "logging";

    private String endpoint;

    private long timeout = 5000;

    private int port = 9494;

    private long readerInterval = 1000;

    @Config("traffic")
    private TrafficConfig trafficConfig;

    @Config("exception")
    private ExceptionConfig exceptionConfig;

    @Getter
    @Setter
    public static class TrafficConfig {

        private boolean enabled = true;

        private boolean gatewayEnabled = true;

        private boolean serviceEnabled = true;

    }

    public static class ExceptionConfig {

        @Getter
        @Setter
        private boolean enabled = true;

        @Getter
        @Setter
        private int maxDepth = 20;

        @Getter
        @Setter
        private Set<String> stackTracePrefixes;

        private transient Inclusion stackTraces;

        public boolean withStackTrace(String className) {
            if (stackTraces == null) {
                stackTraces = new Inclusion(null, stackTracePrefixes);
            }
            return stackTraces.test(className);
        }
    }

    public static class DocumentConfig {

        @Getter
        @Setter
        private boolean enabled = true;

    }
}
