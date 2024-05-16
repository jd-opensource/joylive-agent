/**
 * Represents the application configuration and metadata within a system.
 * This class encapsulates various attributes related to the application,
 * such as its name, instance information, associated service, and location.
 * It also provides methods for accessing and manipulating application metadata.
 * Additionally, it includes constants for key metadata attributes and utility methods
 * for labeling and identifying the application process.
 *
 * @since 1.0.0
 */
package com.jd.live.agent.core.instance;

/**
 * Enumerates the lifecycle states of an application, specifying its ability to handle inbound requests and perform
 * outbound operations in each state.
 * <p>
 * This enum defines the application's lifecycle through four states: STARTING, READY, DESTROYING, and DESTROYED.
 * The {@code inbound()} and {@code outbound()} methods are used to determine the application's capabilities
 * regarding handling inbound requests and performing outbound operations, respectively, in each state.
 * Additionally, the {@code getMessage()} method provides a human-readable description of the application's current state.
 * </p>
 */
public enum AppStatus {

    /**
     * The application is in the process of starting. It cannot accept inbound requests or perform outbound operations.
     */
    STARTING,

    /**
     * The application is fully operational and can both accept inbound requests and perform outbound operations.
     */
    READY {
        @Override
        public boolean inbound() {
            return true;
        }

        @Override
        public boolean outbound() {
            return true;
        }
    },

    /**
     * The application is in the process of shutting down. It cannot accept inbound requests or perform outbound operations.
     */
    DESTROYING,

    /**
     * The application has been shut down and is no longer operational. It cannot accept inbound requests or perform outbound operations.
     */
    DESTROYED;

    /**
     * Determines if the application in its current state can accept inbound requests.
     * <p>
     * By default, applications in the STARTING, DESTROYING, and DESTROYED states cannot accept inbound requests.
     * This method should be overridden in states where inbound request handling is allowed.
     * </p>
     *
     * @return {@code true} if the application can accept inbound requests in the current state; {@code false} otherwise.
     */
    public boolean inbound() {
        return false;
    }

    /**
     * Determines if the application in its current state can perform outbound operations.
     * <p>
     * By default, applications in the STARTING, DESTROYING, and DESTROYED states cannot perform outbound operations.
     * This method should be overridden in states where outbound operations are allowed.
     * </p>
     *
     * @return {@code true} if the application can perform outbound operations in the current state; {@code false} otherwise.
     */
    public boolean outbound() {
        return false;
    }

    /**
     * Provides a human-readable message describing the current state of the application.
     * <p>
     * This method returns a string that reflects the application's current lifecycle state,
     * which can be useful for logging, user notifications, or debugging purposes.
     * </p>
     *
     * @return A string message describing the current state of the application.
     */
    public String getMessage() {
        return "The application is " + this.name().toLowerCase();
    }
}

