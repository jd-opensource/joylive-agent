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
package com.jd.live.agent.core.util.network;

import lombok.Getter;

/**
 * Represents a network address with host and port information.
 * Handles both IPv4 and IPv6 address formats, including bracketed IPv6 notation.
 */
@Getter
public class Address {

    private final String host;

    private final Integer port;

    private final boolean ipv6;

    private final String uriHost;

    private final String address;

    private Address(String host, Integer port, boolean ipv6) {
        this.host = host;
        this.port = port;
        this.ipv6 = ipv6;
        this.uriHost = !ipv6 ? host : ('[' + host + ']');
        this.address = port == null ? uriHost : (uriHost + ":" + port);
    }

    @Override
    public String toString() {
        return address;
    }

    /**
     * Parses an address string with default settings (supports port)
     *
     * @param address The address string to parse
     * @return Parsed Address object, or null for empty input
     */
    public static Address parse(String address) {
        return parse(address, true, null);
    }

    /**
     * Parses an address string with specified port.
     *
     * @param address the address string to parse (may contain port)
     * @param port    the default port to use if not specified in address
     * @return parsed Address object, or null for empty input
     */
    public static Address parse(String address, int port) {
        return parse(address, false, port);
    }

    /**
     * Parses a string address into an Address object.
     *
     * @param address         the address string to parse (may be IPv4, IPv6, or hostname)
     * @param addressWithPort true if the address may contain a port number
     * @param defPort         default port to use if no port is specified
     * @return Address object, or null if input is empty
     */
    public static Address parse(String address, boolean addressWithPort, Integer defPort) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        char startChar = address.charAt(0);
        char endChar = address.charAt(address.length() - 1);
        if (startChar == '[') {
            if (endChar == ']') {
                return new Address(address.substring(1, address.length() - 1), defPort, true);
            }
            int pos = address.lastIndexOf(']');
            if (pos > 0) {
                if (address.charAt(pos + 1) == ':') {
                    try {
                        int p = Integer.parseInt(address.substring(pos + 2));
                        return new Address(address.substring(1, pos), p, true);
                    } catch (NumberFormatException e) {
                        return new Address(address.substring(1, pos), defPort, true);
                    }
                }
                return new Address(address.substring(1, pos), defPort, true);
            }
            return new Address(address.substring(1), defPort, true);
        } else if (endChar == ']') {
            return new Address(address.substring(0, address.length() - 1), defPort, true);
        } else if (addressWithPort) {
            int pos1 = address.lastIndexOf(':');
            if (pos1 >= 0) {
                int pos2 = pos1 < 1 ? -1 : address.lastIndexOf(':', pos1 - 1);
                if (pos2 > 0) {
                    return new Address(address, defPort, true);
                } else {
                    try {
                        Integer p = Integer.parseInt(address.substring(pos1 + 1));
                        return new Address(address.substring(0, pos1), p, false);
                    } catch (NumberFormatException e) {
                        return new Address(address.substring(0, pos1), defPort, false);
                    }
                }
            }
            return new Address(address, defPort, false);
        } else {
            return new Address(address, defPort, address.indexOf(':') >= 0);
        }
    }

}
