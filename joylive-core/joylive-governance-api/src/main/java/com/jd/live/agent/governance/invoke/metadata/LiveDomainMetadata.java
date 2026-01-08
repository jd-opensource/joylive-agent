package com.jd.live.agent.governance.invoke.metadata;

import com.jd.live.agent.governance.event.TrafficEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * The {@code LiveDomainMetadata} class encapsulates the metadata associated with a live domain request.
 * It extends the {@link LiveMetadata} class to provide additional information specific to domain-related
 * operations. This class is typically used to store and retrieve details such as the domain host, unit
 * information, and policy identifiers that are relevant to the processing of live domain requests.
 *
 * <p>Note that this class uses Lombok's {@code @Getter} and {@code @SuperBuilder} annotations to automatically
 * generate getters and a builder pattern for constructing instances of this class.</p>
 *
 * @see LiveMetadata
 */
@Getter
@SuperBuilder
public class LiveDomainMetadata extends LiveMetadata {

    /**
     * The host of the live domain.
     */
    private String host;

    /**
     * The unit host associated with the live domain.
     */
    private String unitHost;

    /**
     * The backend identifier for the unit associated with the live domain.
     */
    private String unitBackend;

    /**
     * The path associated with the unit of the live domain.
     */
    private String unitPath;

    /**
     * The business variable relevant to the live domain request.
     */
    private String bizVariable;

    @Override
    public TrafficEvent configure(TrafficEvent event) {
        return super.configure(event).liveBizVariable(bizVariable);
    }

    private static final class LiveDomainMetadataBuilderImpl extends LiveDomainMetadataBuilder<LiveDomainMetadata, LiveDomainMetadataBuilderImpl> {
    }

    public abstract static class LiveDomainMetadataBuilder<C extends LiveDomainMetadata, B extends LiveDomainMetadataBuilder<C, B>> extends LiveMetadata.LiveMetadataBuilder<C, B> {
    }

}

