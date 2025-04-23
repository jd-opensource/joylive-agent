package com.jd.live.agent.plugin.application.springboot.v2.util.port;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * A class that implements the PortDetector interface by using JMX to determine the port.
 */
public class JmxPortDetector implements PortDetector {

    @Override
    public PortInfo getPort() throws Throwable {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"), Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
        if (objectNames == null || objectNames.isEmpty()) {
            return null;
        }
        ObjectName connector = objectNames.iterator().next();
        String port = connector.getKeyProperty("port");
        if (port == null || port.isEmpty()) {
            return null;
        }
        try {
            String secure = (String) beanServer.getAttribute(connector, "secure");
            return new PortInfo(Integer.parseInt(port), Boolean.parseBoolean(secure));
        } catch (Throwable e) {
            return new PortInfo(Integer.parseInt(port), false);
        }
    }
}
