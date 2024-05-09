package com.jd.live.agent.governance.policy.service.loadbalance;

/**
 * Defines the stickiness type for load balancing or session handling.
 */
public enum StickyType {

    /**
     * No stickiness. Each request may be handled by any available provider
     * without preference.
     */
    NONE,

    /**
     * Preferred stickiness. Requests prefer to be handled by a previously
     * used provider but may fall back to others if necessary.
     */
    PREFERRED,

    /**
     * Fixed stickiness. Requests are strictly bound to a specific provider
     * once they have used it for the first time.
     */
    FIXED
}
