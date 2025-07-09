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
  */
public enum AppStatus {

    /**
     * The application is in the process of starting. It cannot accept inbound requests.
     */
    STARTING,

    /**
     * The application is started.
     */
    STARTED,

    /**
     * The application is fully operational and can both accept inbound requests and perform outbound operations.
     */
    READY {
        @Override
        public boolean inbound() {
            return true;
        }
    },

    /**
     * The application is in the process of shutting down. It cannot accept new inbound requests.
     */
    DESTROYING {
        @Override
        public boolean isDestroy() {
            return true;
        }
    },

    /**
     * The application has been shut down and is no longer operational. It cannot accept inbound requests or perform outbound operations.
     */
    DESTROYED {
        @Override
        public boolean isDestroy() {
            return true;
        }

        @Override
        public boolean outbound() {
            return false;
        }

    };

    public boolean isDestroy() {
        return false;
    }

    /**
     * Determines if the application in its current state can accept inbound requests.
     *
     * @return {@code true} if the application can accept inbound requests in the current state; {@code false} otherwise.
     */
    public boolean inbound() {
        return false;
    }

    /**
     * Determines if the application in its current state can perform outbound operations.
     *
     * @return {@code true} if the application can perform outbound operations in the current state; {@code false} otherwise.
     */
    public boolean outbound() {
        return true;
    }

    /**
     * Provides a human-readable message describing the current state of the application.
     *
     * @return A string message describing the current state of the application.
     */
    public String getMessage() {
        return "The application is " + this.name().toLowerCase();
    }
}

