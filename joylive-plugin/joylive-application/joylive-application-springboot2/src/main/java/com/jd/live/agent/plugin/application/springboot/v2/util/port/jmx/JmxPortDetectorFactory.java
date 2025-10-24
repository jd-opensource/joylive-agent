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
package com.jd.live.agent.plugin.application.springboot.v2.util.port.jmx;

import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Factory for creating port detectors that use JMX to retrieve server port information.
 * Supports Tomcat, Jetty and Undertow servers.
 */
public class JmxPortDetectorFactory implements PortDetectorFactory {

    private final List<PortDetector> detectors = Arrays.asList(new PortDetector[]{
            new TomcatJmxPortDetector(),
            new JettyJmxPortDetector(),
            new UndertowJmxPortDetector()
    });

    @Override
    public PortDetector get(AppContext context) {
        return new CompositeJmxPortDetector(detectors);
    }

    /**
     * Combines multiple port detectors and tries them sequentially until a valid port is found.
     */
    private static class CompositeJmxPortDetector implements PortDetector {
        private final List<PortDetector> detectors;

        CompositeJmxPortDetector(List<PortDetector> detectors) {
            this.detectors = detectors;
        }

        @Override
        public PortInfo getPort() {
            for (PortDetector detector : detectors) {
                PortInfo info = detector.getPort();
                if (info != null) {
                    return info;
                }
            }
            return null;
        }
    }

    /**
     * Base implementation for JMX-based port detection with configurable attributes.
     */
    private static abstract class JmxPortDetector implements PortDetector {
        protected final String objectNamePattern;
        protected final String protocol;
        protected final String portAttribute;
        protected final String secureAttribute;

        JmxPortDetector(String objectNamePattern, String protocol, String portAttribute, String secureAttribute) {
            this.objectNamePattern = objectNamePattern;
            this.protocol = protocol;
            this.portAttribute = portAttribute;
            this.secureAttribute = secureAttribute;
        }

        @Override
        public PortInfo getPort() {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            try {
                Set<ObjectName> objectNames = beanServer.queryNames(
                        new ObjectName(objectNamePattern),
                        Query.match(Query.attr("protocol"), Query.value(protocol))
                );
                if (objectNames == null || objectNames.isEmpty()) {
                    return null;
                }

                ObjectName connector = objectNames.iterator().next();
                String port = connector.getKeyProperty(portAttribute);
                if (port == null || port.isEmpty()) {
                    return null;
                }
                boolean secure = false;
                try {
                    String secureValue = (String) beanServer.getAttribute(connector, secureAttribute);
                    secure = Boolean.parseBoolean(secureValue);
                } catch (Throwable ignored) {
                }
                return new PortInfo(Integer.parseInt(port), secure);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    /**
     * Port detector for Apache Tomcat server using JMX.
     */
    private static class TomcatJmxPortDetector extends JmxPortDetector {
        TomcatJmxPortDetector() {
            super("*:type=Connector,*", "HTTP/1.1", "port", "secure");
        }
    }

    /**
     * Port detector for Eclipse Jetty server using JMX.
     */
    private static class JettyJmxPortDetector extends JmxPortDetector {

        JettyJmxPortDetector() {
            super("org.eclipse.jetty.*:type=connector,*", "HTTP", "port", "ssl");
        }
    }

    /**
     * Port detector for Undertow server using JMX.
     */
    private static class UndertowJmxPortDetector extends JmxPortDetector {

        UndertowJmxPortDetector() {
            super("org.wildfly.*:socket-binding=*,*", "http", "boundPort", "secure");
        }
    }

}
