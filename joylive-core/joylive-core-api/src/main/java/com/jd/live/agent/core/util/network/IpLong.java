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

import java.util.Arrays;

/**
 * Represents an IP as a Long.
 */
public class IpLong implements Comparable<IpLong> {
    /**
     * High 8 bytes of IPV6
     */
    @Getter
    protected long high;
    /**
     * Low 8 bytes of IPV6 or 4 bytes of IPV4
     */
    @Getter
    protected long low;
    /**
     * Type
     */
    @Getter
    protected IpType type;
    /**
     * Interface name
     */
    @Getter
    protected String ifName;
    /**
     * IP information
     */
    protected String ip;

    public IpLong(final String ip) {
        IpPart part = parseIp(ip);
        if (part == null) {
            throw new IllegalArgumentException(String.format("invalid ip %s", ip));
        }
        this.ip = ip;
        this.type = part.type;
        int index = 0;
        int[] parts = part.parts;
        if (parts.length == 8) {
            //ipv6
            high += ((long) parts[index++]) << 48;
            high += ((long) (parts[index++]) << 32);
            high += ((long) (parts[index++]) << 16);
            high += parts[index++];
            low += ((long) parts[index++]) << 48;
            low += ((long) (parts[index++]) << 32);
            low += ((long) (parts[index++]) << 16);
            low += parts[index];
        } else {
            high = -1;
            //ipv4
            low += ((long) parts[index++]) << 24;
            low += ((long) (parts[index++]) << 16);
            low += ((long) (parts[index++]) << 8);
            low += parts[index];
        }
    }

    public IpLong(long low) {
        this.high = -1;
        this.low = low;
        this.type = IpType.IPV4;
    }

    public IpLong(long high, long low) {
        this(high, low, IpType.IPV6, null);
    }

    public IpLong(long high, long low, IpType type) {
        this(high, low, type, null);
    }

    public IpLong(long high, long low, IpType type, String ifName) {
        this.high = high;
        this.low = low;
        this.type = type;
        this.ifName = ifName;
    }

    @Override
    public int compareTo(final IpLong o) {
        if (o == null) {
            return 1;
        } else if (high > o.high) {
            return 1;
        } else if (high < o.high) {
            return -1;
        } else return Long.compare(low, o.low);
    }

    protected StringBuilder append(final StringBuilder builder, final long value) {
        String st = Long.toHexString(value);
//        switch (st.length()) {
//            case 1:
//                builder.append("000").append(st);
//                break;
//            case 2:
//                builder.append("00").append(st);
//                break;
//            case 3:
//                builder.append("0").append(st);
//                break;
//            default:
//                builder.append(st);
//                break;
//        }
        builder.append(st);
        return builder;
    }

    @Override
    public String toString() {
        if (ip == null) {
            StringBuilder builder = new StringBuilder(40);
            if (high < 0) {
                //ipv4
                builder.append((low & 0xFFFFFFFFL) >>> 24).append('.')
                        .append((low & 0xFFFFFFL) >>> 16).append('.')
                        .append((low & 0xFFFFL) >>> 8).append('.')
                        .append(low & 0xFFL);
            } else {
                //ipv6，Automatically abbreviates 2 or more occurrences of "0000:0000"
                int[] parts = new int[8];
                parts[0] = (int) ((high) >>> 48);
                parts[1] = (int) ((high & 0xFFFFFFFFFFFL) >>> 32);
                parts[2] = (int) ((high & 0xFFFFFFFFL) >>> 16);
                parts[3] = (int) ((high & 0xFFFFL));
                parts[4] = (int) ((low) >>> 48);
                parts[5] = (int) ((low & 0xFFFFFFFFFFFFL) >>> 32);
                parts[6] = (int) ((low & 0xFFFFFFFFL) >>> 16);
                parts[7] = (int) (low & 0xFFFFL);
                int end = type == IpType.IPV6 ? 7 : 5;
                int zero = 0;
                int zeroIndex = -1;
                int maxZero = 0;
                int maxZeroIndex = 0;
                for (int i = 0; i <= end; i++) {
                    if (parts[i] == 0) {
                        if (zero++ == 0) {
                            zeroIndex = i;
                        }
                    } else if (zero > 0) {
                        if (maxZero < zero) {
                            maxZero = zero;
                            maxZeroIndex = zeroIndex;
                        }
                        zero = 0;
                        zeroIndex = -1;
                    }
                }
                if (zero > 0 && maxZero < zero) {
                    maxZero = zero;
                    maxZeroIndex = zeroIndex;
                }
                if (maxZero < 2) {
                    maxZero = 0;
                    maxZeroIndex = 0;
                }
                for (int i = 0; i < maxZeroIndex; i++) {
                    append(builder.append(i > 0 ? ":" : ""), parts[i]);
                }
                if (maxZero > 0) {
                    builder.append("::");
                }
                for (int i = maxZeroIndex + maxZero; i <= end; i++) {
                    append(builder, parts[i]).append(i < 7 ? ":" : "");
                }
                if (type == IpType.MIXER) {
                    // mixer
                    builder.append(parts[6] >>> 8).append('.');
                    builder.append(parts[6] & 0xFF).append('.');
                    builder.append(parts[7] >>> 8).append('.');
                    builder.append(parts[7] & 0xFF);
                }
                if (ifName != null) {
                    builder.append('%').append(ifName);
                }
            }
            ip = builder.toString();
        }
        return ip;
    }

    /**
     * Decomposes the IP address.
     *
     * <p>Conversion between IPV6 and IPV4
     * <p>In compatible situations: If the ipv4 is represented as "X.X.X.X", the corresponding ipv6 will be "::X.X.X.X" (zero-padding the high bits)
     * <p>In mapping situations: If the ipv6 is represented as "::FFFF:X.X.X.X" (bits 33-128 are ::FFFF), in these cases, the ipv6 will be mapped to ipv4
     *
     * @param ip IP address
     * @return Segmented IP
     */
    public static IpPart parseIp(final String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        IpType ipType = null;
        int[] parts = new int[8];
        int index = 0;
        int start = -1;
        int end = -1;
        int part;
        int colon = 0;
        int ellipsis = -1; // Placeholder index
        int ipv4Index = -1; // Starting position for ipv4, used in mixed compatibility scenarios
        char[] chars = ip.toCharArray();
        char ch = 0;
        String ifName = null;
        for (int i = 0; i < chars.length; i++) {
            ch = chars[i];
            switch (ch) {
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    if (ipType == IpType.IPV4 || ipType == IpType.MIXER) {
                        // IPv6 cannot appear after IPv4
                        return null;
                    }
                    ipType = IpType.IPV6;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (start == -1) {
                        start = i;
                    }
                    end = i;
                    if (end - start > 4) {
                        // Tool long
                        return null;
                    }
                    colon = 0;
                    break;
                case '.':
                    if (start == -1) {
                        // There must be a character in front
                        return null;
                    }
                    if (ipv4Index == -1) {
                        // Starting position of IPv4
                        ipv4Index = index;
                    }
                    // Mix mode
                    ipType = ipType == null || ipType == IpType.IPV4 ? IpType.IPV4 : IpType.MIXER;
                    part = Integer.parseInt(new String(chars, start, end - start + 1));
                    if (part > 0xFF) {
                        // Each part of an IPv4 address can be a maximum of 255
                        return null;
                    }
                    parts[index++] = part;
                    start = -1;
                    end = -1;
                    break;
                case ':':
                    if (ipType == IpType.IPV4 || ipType == IpType.MIXER) {
                        // IPv6 cannot appear after IPv4
                        return null;
                    }
                    ipType = IpType.IPV6;
                    // Count the number of colons
                    switch (++colon) {
                        case 1:
                            if (i == 0) {
                                //The first character needs to be followed by another one ':'
                                if (i >= chars.length - 1 || chars[i] != ':') {
                                    return null;
                                }
                            } else {
                                part = Integer.parseInt(new String(chars, start, end - start + 1), 16);
                                if (part > 0xFFFF) {
                                    // Each part of an IPv6 address can be a maximum of 65535
                                    return null;
                                }
                                parts[index++] = part;
                                start = -1;
                                end = -1;
                            }
                            break;
                        case 2:
                            //Only one "::" can appear in an IPv6 address.
                            if (ellipsis >= 0) {
                                return null;
                            }
                            ellipsis = index;
                            parts[index++] = -1;
                            break;
                        default:
                            return null;
                    }
                    break;
                case '%':
                    if (ipType != IpType.IPV6 && ipType != IpType.MIXER || colon > 0) {
                        // Must be IPv6, cannot start with ':'.
                        return null;
                    }
                    part = Integer.parseInt(new String(chars, start, end - start + 1), 16);
                    if (part > 0xFFFF) {
                        // Each part of an IPv6 address can be a maximum of 65535
                        return null;
                    }
                    parts[index++] = part;
                    start = -1;
                    end = -1;
                    ifName = new String(chars, i + 1, chars.length - i - 1);
                    break;
                default:
                    return null;
            }
            if (ifName != null) {
                break;
            }
        }
        if ((start == -1 && colon == 0 && (ifName == null || ifName.isEmpty())) || colon == 1) {
            // Ends with '.' or ':' or the network interface is empty
            return null;
        } else if (start > -1) {
            // End with digit
            part = Integer.parseInt(new String(chars, start, end - start + 1), ipType == IpType.IPV4 || ipType == IpType.MIXER ? 10 : 16);
            if (part > (ipType == IpType.IPV4 || ipType == IpType.MIXER ? 0xFF : 0xFFFF)) {
                // IPv4 preferred
                return null;
            }
            parts[index++] = part;
            if (ipType == IpType.IPV4) {
                // Pure ipv4
                return index == 4 ? new IpPart(ipType, Arrays.copyOfRange(parts, 0, 4), ifName) : null;
            } else if (ipType == IpType.MIXER) {
                // Mix mode
                if (index - ipv4Index < 4) {
                    // IPv4 has less than 4 segments.
                    return null;
                }
                // ipv6
                parts[ipv4Index] = (parts[ipv4Index] << 8) | parts[ipv4Index + 1];
                parts[ipv4Index + 1] = (parts[ipv4Index + 2] << 8) | parts[ipv4Index + 3];
                index -= 2;
            }
        }
        // ipv6 or mixer
        if (index > 8) {
            return null;
        } else if (ellipsis == -1) {
            return index == 8 ? new IpPart(ipType, parts, ifName) : null;
        }
        int[] result = new int[8];
        // Before the ellipsis
        for (int i = 0; i < ellipsis; i++) {
            result[i] = parts[i];
        }
        // Represented by the ellipsis
        int max = ellipsis + (7 - index + 1);
        for (int i = ellipsis; i <= max; i++) {
            result[i] = 0x0000;
        }
        // After the ellipsis
        for (int i = ellipsis + 1; i < index; i++) {
            result[max + i - ellipsis] = parts[i];
        }
        return new IpPart(ipType, result, ifName);
    }

}
