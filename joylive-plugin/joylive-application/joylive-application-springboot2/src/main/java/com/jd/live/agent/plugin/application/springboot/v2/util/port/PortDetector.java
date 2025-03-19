package com.jd.live.agent.plugin.application.springboot.v2.util.port;

/**
 * An interface that defines a method to get the port number.
 */
public interface PortDetector {
    /**
     * Returns the port number.
     *
     * @return The port number as an Integer, or null if the port cannot be determined.
     * @throws Throwable If an error occurs while trying to determine the port.
     */
    Integer getPort() throws Throwable;
}
