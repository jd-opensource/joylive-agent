package com.jd.live.agent.core.mcp;

/**
 * Interface for validating objects.
 * Implementations should provide validation logic for specific object types.
 *
 * @since 1.0
 */
public interface Validator {

    /**
     * Validates the provided object.
     *
     * @param value The object to validate
     * @throws Exception If validation fails
     */
    void validate(Object value) throws Exception;

}
