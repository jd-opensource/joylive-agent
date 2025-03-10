package com.jd.live.agent.plugin.application.springboot.v2.uti.port;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * A factory class that provides a PortDetector instance based on the given ConfigurableApplicationContext.
 */
public class PortDetectorFactory {

    /**
     * Returns a PortDetector instance based on the given ConfigurableApplicationContext.
     *
     * @param context The ConfigurableApplicationContext.
     * @return A PortDetector instance.
     */
    public static PortDetector get(ConfigurableApplicationContext context) {
        return new JmxPortDetector();
    }

}
