package com.jd.live.agent.governance.invoke.metadata.parser;

import com.jd.live.agent.governance.invoke.metadata.LaneMetadata;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.policy.live.UnitRule;
import com.jd.live.agent.governance.policy.service.ServicePolicy;

/**
 * The {@code MetadataParser} interface defines a contract for parsing metadata of different
 * types within a service framework. Each implementing class is expected to provide a
 * specific implementation of the {@code parse} method to handle the parsing logic for
 * the particular type of metadata it represents.
 *
 * @param <T> the type of metadata that the implementing parser will produce
 */
public interface MetadataParser<T> {

    /**
     * Parses the relevant metadata based on the context and configuration.
     *
     * @return an instance of the metadata, fully parsed and configured
     */
    T parse();

    /**
     * The {@code ServiceParser} interface extends {@code MetadataParser} and is specifically
     * designed for parsing service metadata ({@code ServiceMetadata}). It also provides a
     * default method for configuring the metadata with a unit rule, which by default does
     * nothing and can be overridden by implementations that require custom configuration logic.
     */
    interface ServiceParser extends MetadataParser<ServiceMetadata> {

        /**
         * Configures the provided {@code ServiceMetadata} with the given {@code UnitRule}.
         * The default implementation returns the metadata as is, without any modification.
         * This method can be overridden by subclasses that need to perform additional
         * configuration based on the unit rule.
         *
         * @param metadata      the service metadata to be configured
         * @param unitRule      the unit rule to be applied for configuration
         * @return              the configured service metadata
         */
        default ServiceMetadata configure(ServiceMetadata metadata, UnitRule unitRule) {
            return metadata;
        }
    }

    /**
     * The {@code LiveParser} interface extends {@code MetadataParser} and specializes in
     * parsing live metadata ({@code LiveMetadata}). It includes a default method for
     * configuring the metadata with a service policy, which by default returns the metadata
     * unchanged. This method can be overridden by implementations that require specific
     * configuration logic based on the service policy.
     */
    interface LiveParser extends MetadataParser<LiveMetadata> {

        /**
         * Configures the provided {@code LiveMetadata} with the given {@code ServicePolicy}.
         * The default implementation returns the metadata as is, without any modification.
         * This method can be overridden by subclasses that need to perform custom configuration
         * based on the service policy.
         *
         * @param metadata      the live metadata to be configured
         * @param servicePolicy the service policy to be applied for configuration
         * @return              the configured live metadata
         */
        default LiveMetadata configure(LiveMetadata metadata, ServicePolicy servicePolicy) {
            return metadata;
        }
    }

    /**
     * The {@code LaneParser} interface extends {@code MetadataParser} and is used for parsing
     * lane metadata ({@code LaneMetadata}). At this time, it does not define any additional
     * methods or default behavior beyond what is provided by the {@code MetadataParser}
     * interface.
     */
    interface LaneParser extends MetadataParser<LaneMetadata> {

    }

}

