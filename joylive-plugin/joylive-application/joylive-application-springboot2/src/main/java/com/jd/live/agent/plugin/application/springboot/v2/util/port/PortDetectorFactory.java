package com.jd.live.agent.plugin.application.springboot.v2.util.port;

import com.jd.live.agent.core.bootstrap.AppContext;

/**
 * A factory class that provides a PortDetector instance based on the given ConfigurableApplicationContext.
 */
public class PortDetectorFactory {

    /**
     * Returns a PortDetector instance based on the given AppContext.
     *
     * @param context The AppContext.
     * @return A PortDetector instance.
     */
    public static PortDetector get(AppContext context) {
        return new JmxPortDetector();
    }

}
