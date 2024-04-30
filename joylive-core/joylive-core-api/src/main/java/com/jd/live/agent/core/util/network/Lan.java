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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static com.jd.live.agent.core.util.StringUtils.split;

/**
 * Local area network, multiple segments separated by commas or semicolons
 * <p>
 * Format for a single segment:
 * <p>172.168.1.0/24
 * <p>172.168.1.0-172.168.1.255
 * <p>172.168.1.1
 * <p>172.168.1.*
 */
@Getter
public class Lan {
    /**
     * The list of segments in the LAN.
     */
    private final List<Segment> segments = new ArrayList<>();
    /**
     * The ID of the LAN.
     */
    private final int id;
    /**
     * The name of the LAN.
     */
    private final String name;

    /**
     * Construct a LAN object with the specified IP addresses.
     *
     * @param ips The IP addresses
     */
    public Lan(String ips) {
        this(0, null, ips, false);
    }

    /**
     * Construct a LAN object with the specified IP addresses and error handling option.
     *
     * @param ips         The IP addresses
     * @param ignoreError Whether to ignore errors while parsing IP addresses
     */
    public Lan(String ips, boolean ignoreError) {
        this(0, null, ips, ignoreError);
    }

    /**
     * Construct a LAN object with the specified ID, name, and IP addresses.
     *
     * @param id   The ID
     * @param name The name
     * @param ips  The IP addresses
     */
    public Lan(int id, String name, String ips) {
        this(id, name, ips, false);
    }

    /**
     * Construct a LAN object with the specified ID, name, IP addresses, and error handling option.
     *
     * @param id          The ID
     * @param name        The name
     * @param ips         The IP addresses
     * @param ignoreError Whether to ignore errors while parsing IP addresses
     */
    public Lan(int id, String name, String ips, boolean ignoreError) {
        this.id = id;
        this.name = name;
        if (ips != null && !ips.isEmpty()) {
            String[] parts = split(ips, SEMICOLON_COMMA_WHITESPACE);
            for (String part : parts) {
                try {
                    segments.add(new Segment(part));
                } catch (IllegalArgumentException e) {
                    if (!ignoreError) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Check if the LAN contains the specified IP address.
     *
     * @param ip The IP address
     * @return True if the LAN contains the IP address, false otherwise
     */
    public boolean contains(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        IpLong ipLong = new IpLong(ip);
        return contains(segment -> segment.contains(ipLong));
    }

    /**
     * Check if the LAN contains the specified IP address based on the provided predicate.
     *
     * @param predicate The predicate to determine if the IP is contained
     * @return True if the LAN contains the IP address, false otherwise
     */
    protected boolean contains(final Predicate<Segment> predicate) {
        if (segments.isEmpty()) {
            // Not specified, so it's the entire network
            return true;
        }
        for (Segment segment : segments) {
            if (predicate.test(segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the LAN contains the specified IP address.
     *
     * @param ip The IP address
     * @return True if the LAN contains the IP address, false otherwise
     */
    public boolean contains(IpLong ip) {
        return contains(segment -> segment.contains(ip));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Lan lan = (Lan) o;

        if (id != lan.id) {
            return false;
        }
        if (!segments.equals(lan.segments)) {
            return false;
        }
        return Objects.equals(name, lan.name);

    }

    @Override
    public int hashCode() {
        int result = segments.hashCode();
        result = 31 * result + id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(100);
        int count = 0;
        for (Segment segment : segments) {
            if (count++ > 0) {
                builder.append(';');
            }
            builder.append(segment.toString());
        }
        return builder.toString();
    }
}
