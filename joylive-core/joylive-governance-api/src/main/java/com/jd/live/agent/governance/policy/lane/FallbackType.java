package com.jd.live.agent.governance.policy.lane;

/**
 * The type of rollback policy for lane redirection, when the specified lane instance can not be matched
 *
 * @since 1.6.0
 */
public enum FallbackType {

    /**
     * Lane redirects to Baseline Lane
     */
    DEFAULT,

    /**
     * Request denied without redirection
     */
    REJECT,

    /**
     * Lane redirection to a custom lane, combined with the `fallbackLane` property
     */
    CUSTOM;
}