/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.implement.service.config.apollo.client;

import com.jd.live.agent.core.util.URI;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.StringUtils.join;
import static com.jd.live.agent.core.util.StringUtils.split;

/**
 * Represents an Apollo address, which consists of an address type and a URL.
 * This class is used to store and manage the address information for Apollo-related services.
 * It supports parsing multi-address strings and distinguishing between Meta Server and Config Server addresses.
 */
@Getter
public class ApolloAddress {

    /**
     * Schema identifier for Meta Server addresses.
     */
    private static final String SCHEMA_META = "meta";

    /**
     * Schema identifier for Meta Server addresses.
     */
    private static final String SCHEMA_METAS = "metas";

    /**
     * Schema identifier for HTTP addresses.
     */
    private static final String SCHEMA_HTTP = "http";

    /**
     * Schema identifier for HTTPS addresses.
     */
    private static final String SCHEMA_HTTPS = "https";

    /**
     * The type of the address, which indicates the role or purpose of the address.
     * For example, it could be a Meta Server or Config Server address.
     */
    private final AddressType type;

    /**
     * The URL associated with the address, which specifies the location of the service.
     * This URL is used to connect to the corresponding Apollo service.
     */
    private final String address;

    /**
     * Constructs a new ApolloAddress instance with the specified type and address.
     *
     * @param type    the type of the address, such as {@link AddressType#META_SERVER} or {@link AddressType#CONFIG_SERVER}.
     * @param address the URL associated with the address.
     */
    public ApolloAddress(AddressType type, String address) {
        this.type = type;
        this.address = address;
    }

    /**
     * Parses a multi-address string and returns an ApolloAddress instance.
     * The method distinguishes between Meta Server and Config Server addresses based on the schema.
     * If a Meta Server address is found, it takes precedence. Otherwise, Config Server addresses are combined.
     *
     * @param address the multi-address string to parse, e.g., "meta://example.com,http://config1.com,http://config2.com".
     * @return an ApolloAddress instance representing the parsed address.
     * @throws IllegalArgumentException if the address string is invalid or cannot be parsed.
     */
    public static ApolloAddress parse(String address) {
        // parse multi-address
        String[] urls = split(address, ',');
        URI metaServer = null;
        List<URI> configServers = new ArrayList<>(8);
        for (String url : urls) {
            URI uri = URI.parse(url);
            String schema = uri.getSchema();
            if (SCHEMA_META.equalsIgnoreCase(schema)) {
                metaServer = uri.schema(SCHEMA_HTTP);
                break;
            } else if (SCHEMA_METAS.equalsIgnoreCase(schema)) {
                metaServer = uri.schema(SCHEMA_HTTPS);
                break;
            } else if (schema == null || schema.isEmpty()) {
                configServers.add(uri.schema(SCHEMA_HTTP));
            } else {
                configServers.add(uri);
            }
        }
        if (metaServer != null) {
            return new ApolloAddress(AddressType.META_SERVER, metaServer.toString());
        } else {
            String url = join(configServers, ',', (char) 0, (char) 0, false);
            return new ApolloAddress(AddressType.CONFIG_SERVER, url);
        }
    }

}
