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
package com.jd.live.agent.plugin.application.springboot.v2.util.port.env;

import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;

/**
 * A factory class that provides a PortDetector instance based on the given AppContext.
 */
public class EnvPortDetectorFactory implements PortDetectorFactory {

    /**
     * Returns a PortDetector instance based on the given AppContext.
     *
     * @param context The AppContext.
     * @return A PortDetector instance.
     */
    public PortDetector get(AppContext context) {
        return new EnvPortDetector(context);
    }

    private static class EnvPortDetector implements PortDetector {

        private final AppContext context;

        EnvPortDetector(AppContext context) {
            this.context = context;
        }

        @Override
        public PortInfo getPort() {
            String serverPort = context.getProperty("server.port");
            serverPort = serverPort == null || serverPort.isEmpty() ? "8080" : serverPort;
            int port;
            try {
                port = Integer.parseInt(serverPort);
                port = port > 65535 || port <= 0 ? 8080 : port;
            } catch (NumberFormatException e) {
                port = 8080;
            }
            return new PortInfo(port, false);
        }
    }

}
