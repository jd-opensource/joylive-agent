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
    public Integer getPort() throws Throwable {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"), Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
        String value = objectNames.isEmpty() ? null : objectNames.iterator().next().getKeyProperty("port");
        return value == null ? null : Integer.valueOf(value);
    }
}
