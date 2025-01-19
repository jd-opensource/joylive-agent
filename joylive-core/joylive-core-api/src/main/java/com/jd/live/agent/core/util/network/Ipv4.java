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

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Utility class for working with IPv4 and IPv6 addresses.
 * <p>
 * This class provides methods for identifying local IP addresses, checking if an IP is within a specified network segment,
 * converting IP addresses to string representations, and more. It supports both IPv4 and IPv6 addressing schemes.
 * </p>
 * <p>
 * Key features include determining the local IP address, validating IP addresses against network segments, and converting
 * network interface information to human-readable forms.
 * </p>
 */
public class Ipv4 {

    /**
     * Management IP
     */
    public static String MANAGE_IP;
    /**
     * Network card
     */
    public static String NET_INTERFACE;
    /**
     * The IP address used for binding to any available address on the host.
     */
    public static final String ANYHOST = "0.0.0.0";
    /**
     * A predefined network segment representing common internal (LAN) IP address ranges.
     */
    public static final Lan INTRANET = new Lan("172.16.0.0/12;192.168.0.0/16;10.0.0.0/8");
    /**
     * Local bound network card
     */
    public static final String LOCAL_NIC_KEY = "LOCAL_NIC";
    /**
     * Management IP
     */
    public static final String MANAGE_IP_KEY = "MANAGE_IP";
    /**
     * Force IPv6
     */
    public static final String FORCE_IPV_6_STACK = "java.net.forceIPv6Stack";
    /**
     * The minimum value for a valid port number.
     */
    public static final int MIN_PORT = 0;
    /**
     * The minimum value for a valid user-defined port number (above well-known ports).
     */
    public static final int MIN_USER_PORT = 1025;
    /**
     * The maximum value for a valid port number.
     */
    public static final int MAX_PORT = 65535;
    /**
     * Maximum user port
     */
    public static final int MAX_USER_PORT = 65534;
    /**
     * All local IPs
     */
    protected static Set<String> LOCAL_IPS;
    /**
     * Local host names
     */
    protected static Set<String> LOCAL_HOST = new HashSet<>(5);
    /**
     * Preferred local IP
     */
    protected static String LOCAL_IP;

    protected static boolean IPV4 = false;
    /**
     * All network interface information
     */
    protected static Map<String, List<String>> INTERFACES;

    public static final IpLong IP_MIN;

    public static final IpLong IP_MAX;

    static {
        // Get default network card and management network from environment variables
        Map<String, String> env = System.getenv();
        NET_INTERFACE = System.getProperty(LOCAL_NIC_KEY, env.get(LOCAL_NIC_KEY));
        MANAGE_IP = System.getProperty(MANAGE_IP_KEY, env.get(MANAGE_IP_KEY));
        // Check if forcing IPv6 for debugging purposes
        boolean forceIpv6 = false;
        try {
            forceIpv6 = Boolean.parseBoolean(System.getProperty(FORCE_IPV_6_STACK, env.get(FORCE_IPV_6_STACK)));
        } catch (Exception ignored) {
        }
        if (!forceIpv6) {
            try {
                // java.net.preferIPv6Addresses indicates whether to prefer returning IPv6 addresses over IPv4
                // when querying local or remote IP addresses, default is false
                IPV4 = InetAddress.getLocalHost() instanceof Inet4Address;
            } catch (UnknownHostException ignored) {
            }
        } else {
            IPV4 = false;
        }
        System.setProperty("java.net.preferIPv4Stack", String.valueOf(IPV4));
        System.setProperty("java.net.preferIPv6Addresses", String.valueOf(!IPV4));
        IP_MIN = IPV4 ? new IpLong("0.0.0.0") : new IpLong("0000:0000:0000:0000:0000:0000:0000:0000");
        IP_MAX = IPV4 ? new IpLong("255.255.255.255") : new IpLong("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF");
        try {
            LOCAL_IPS = new HashSet<>(getLocalIps());
            LOCAL_IPS.add("127.0.0.1");
            LOCAL_IPS.add("0:0:0:0:0:0:0:1");
            LOCAL_IPS.add("0000:0000:0000:0000:0000:0000:0000:0001");
            LOCAL_IPS.add("::1");
            LOCAL_HOST.add("localhost");
            LOCAL_HOST.add("127.0.0.1");
            LOCAL_HOST.add("0:0:0:0:0:0:0:1");
            LOCAL_HOST.add("0000:0000:0000:0000:0000:0000:0000:0001");
            LOCAL_HOST.add("::1");
            if (NET_INTERFACE != null && !NET_INTERFACE.isEmpty()) {
                LOCAL_IP = getLocalIp(NET_INTERFACE, MANAGE_IP);
            }
            if (LOCAL_IP == null) {
                LOCAL_IP = getLocalIp("en0", MANAGE_IP);
                if (LOCAL_IP == null) {
                    LOCAL_IP = getLocalIp("eth0", MANAGE_IP);
                }
            }
        } catch (SocketException ignored) {
        }
    }

    /**
     * Checks whether the system prefers IPv4 stack.
     *
     * @return {@code true} if the system prefers IPv4 stack, {@code false} otherwise.
     */
    public static boolean isIpv4() {
        return IPV4;
    }

    /**
     * Checks if the given string represents a valid IPv4 address.
     *
     * @param ip The string to check.
     * @return true if the string represents a valid IPv4 address, false otherwise.
     */
    public static boolean isIpv4(String ip) {
        if (ip == null) {
            return false;
        }
        int count = 0;
        int start = -1;
        char ch;
        int length = ip.length();
        for (int i = 0; i < length; i++) {
            ch = ip.charAt(i);
            switch (ch) {
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
                    continue;
                case '.':
                    if (start < 0 || Integer.parseInt(ip.substring(start, i)) > 255) {
                        return false;
                    }
                    count++;
                    if (count > 4) {
                        return false;
                    }
                    start = i + 1;
                    continue;
                default:
                    return false;
            }
        }
        if (count != 3) {
            return false;
        } else if (start >= length || start < 0) {
            return false;
        }
        return Integer.parseInt(ip.substring(start, length)) <= 255;
    }

    /**
     * Determines if the specified host is an any-host address (0.0.0.0).
     *
     * @param host The host to check.
     * @return {@code true} if the host is an any-host address, {@code false} otherwise.
     */
    public static boolean isAnyHost(String host) {
        return ANYHOST.equals(host);
    }

    /**
     * Checks if the specified IP address is considered a local IP address.
     *
     * @param ip The IP address to check.
     * @return {@code true} if the IP address is local, {@code false} otherwise.
     */
    public static boolean isLocalIp(final String ip) {
        return ip != null && LOCAL_IPS != null && LOCAL_IPS.contains(ip);
    }

    /**
     * Get all addresses of the local machine
     *
     * @return All addresses of the local machine
     * @throws SocketException Network exception
     */
    public static List<String> getLocalIps() throws SocketException {
        return getLocalIps(null, null);
    }

    /**
     * Get the addresses on the network interface
     *
     * @return The list of addresses
     * @throws SocketException Network exception
     */
    protected static Map<String, List<String>> getInterfaces() throws SocketException {
        if (INTERFACES == null) {
            Map<String, List<String>> result = new LinkedHashMap<>();
            NetworkInterface ni;
            Enumeration<InetAddress> ias;
            InetAddress address;
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            List<String> ips;
            while (netInterfaces.hasMoreElements()) {
                ni = netInterfaces.nextElement();
                ias = ni.getInetAddresses();
                ips = new ArrayList<>(4);
                while (ias.hasMoreElements()) {
                    address = ias.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (IPV4 && address instanceof Inet4Address
                                || !IPV4 && address instanceof Inet6Address) {
                            ips.add(toIp(address));
                        }
                    }
                }
                if (!ips.isEmpty()) {
                    result.put(ni.getName(), ips);
                }
            }
            INTERFACES = result;
        }
        return INTERFACES;
    }

    /**
     * Retrieves a list of local IP addresses based on the specified network interface and exclusion criteria.
     *
     * @param nic     The network interface to query.
     * @param exclude An IP prefix to exclude from the results.
     * @return A list of local IP addresses.
     * @throws SocketException If an I/O error occurs.
     */
    public static List<String> getLocalIps(final String nic, final String exclude) throws SocketException {
        Map<String, List<String>> interfaces = getInterfaces();
        List<String> result;
        if (nic != null && !nic.isEmpty()) {
            result = interfaces.get(nic);
            result = result == null ? new ArrayList<>() : result;
        } else {
            result = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : interfaces.entrySet()) {
                result.addAll(entry.getValue());
            }
        }
        int count = result.size();
        if (count <= 1) {
            return result;
        }
        if (exclude != null && !exclude.isEmpty()) {
            String ip;
            for (int i = count - 1; i >= 0; i--) {
                ip = result.get(i);
                if (ip.startsWith(exclude)) {
                    result.remove(i);
                    count--;
                    if (count == 1) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get the local LAN address
     *
     * @param nic      The network interface card
     * @param manageIp The management segment IP address
     * @return The local address
     * @throws SocketException SocketException
     */
    public static String getLocalIp(final String nic, final String manageIp) throws SocketException {
        List<String> ips = getLocalIps(nic, manageIp);
        if (!ips.isEmpty()) {
            if (ips.size() == 1) {
                return ips.get(0);
            }
            for (String ip : ips) {
                if (INTRANET.contains(ip)) {
                    return ip;
                }
            }
            return ips.get(0);
        }
        return null;
    }

    /**
     * Get the local LAN address
     *
     * @param manageIp The management segment IP address
     * @return The local address
     * @throws SocketException SocketException
     */
    public static String getLocalIp(final String manageIp) throws SocketException {
        return getLocalIp(NET_INTERFACE, manageIp);
    }

    /**
     * Retrieves the local IP address of the current machine. If the IP address has not been previously determined,
     * it attempts to resolve it using the network interface and management IP settings.
     * If no network interface is specified or an error occurs while attempting to resolve the IP address,
     * it defaults to the loopback address "127.0.0.1".
     *
     * @return The local IP address as a String.
     */
    public static String getLocalIp() {
        if (LOCAL_IP == null) {
            try {
                LOCAL_IP = getLocalIp(NET_INTERFACE, MANAGE_IP);
                if (LOCAL_IP == null) {
                    LOCAL_IP = "127.0.0.1";
                }
            } catch (SocketException ignored) {
            }
        }
        return LOCAL_IP;
    }

    /**
     * Gets the local IP address based on a specified remote address by attempting a socket connection.
     *
     * @param remote The remote address to connect to.
     * @return The local IP address determined from the socket connection.
     */
    public static String getLocalIp(final InetSocketAddress remote) {
        if (remote == null) {
            return getLocalIp();
        }
        if (LOCAL_IP == null) {
            try {
                InetAddress address = getLocalAddress(remote);
                LOCAL_IP = address.getHostAddress();
            } catch (IOException e) {
                getLocalIp();
            }
        }
        return LOCAL_IP;
    }

    /**
     * Retrieves the hostname of the local machine. This method attempts to determine the hostname by querying the network
     * address of the localhost. If the hostname cannot be resolved due to an unknown host exception, the method returns null.
     *
     * @return The hostname of the local machine as a String. If the hostname cannot be determined, returns null.
     */
    public static String getLocalHost() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Checks if it is a local hostname
     *
     * @param host The host
     * @return Local hostname indicator
     */
    public static boolean isLocalHost(final String host) {
        // Returns true if the host is null, empty, or in the local host list; otherwise, returns false
        return host == null || host.isEmpty() || LOCAL_HOST.contains(host);
    }

    /**
     * Get the representation of all local addresses
     *
     * @return Representation of all local addresses
     */
    public static String getAnyHost() {
        // If it's an IPv4 address, return "0.0.0.0"; otherwise, return "0:0:0:0:0:0:0:0"
        return IPV4 ? "0.0.0.0" : "0:0:0:0:0:0:0:0";
    }

    /**
     * Validates whether a port number is within the valid range for TCP/UDP ports.
     *
     * @param port The port number to validate.
     * @return {@code true} if the port is within the valid range, {@code false} otherwise.
     */
    public static boolean isValidPort(final int port) {
        return port <= MAX_PORT && port >= MIN_PORT;
    }

    /**
     * Check if the port is a valid user port (1025-65534)
     *
     * @param port The port
     * @return Whether it is valid
     */
    public static boolean isValidUserPort(final int port) {
        // Returns true if the port is within the range of valid user ports (1025-65534); otherwise, returns false
        return port <= MAX_USER_PORT && port >= MIN_USER_PORT;
    }

    /**
     * Get the local network address by connecting to a remote address
     *
     * @param remote The remote address
     * @return The local network address
     * @throws IOException IO exception
     */
    public static InetAddress getLocalAddress(final InetSocketAddress remote) throws IOException {
        if (remote == null) {
            return null;
        }
        // Connect to the remote address
        try (Socket socket = new Socket()) {
            socket.connect(remote, 1000);
            // Get the local address
            return socket.getLocalAddress();
        }
    }

    /**
     * Determine if the IP is within the network segment
     *
     * @param segments The network segments
     * @param ip       The IP
     * @return Whether the IP is within the network segment
     */
    public static boolean contains(final List<Segment> segments, final String ip) {
        if (segments == null || segments.isEmpty() || ip == null || ip.isEmpty()) {
            return false;
        }
        try {
            IpLong v = new IpLong(ip);
            for (Segment segment : segments) {
                if (segment.contains(v)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Determine if the IP is within the network segment
     *
     * @param segments The network segments
     * @param ip       The IP
     * @return A boolean value
     */
    public static boolean contains(final List<Segment> segments, final long ip) {
        if (segments == null || segments.isEmpty()) {
            return false;
        }
        IpLong ipLong = new IpLong(ip);
        for (Segment segment : segments) {
            if (segment.contains(ipLong)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a {@link SocketAddress} to a string representation, handling IPv6 addresses appropriately.
     *
     * @param address The address to convert.
     * @return A string representation of the address.
     */
    public static String toAddress(final SocketAddress address) {
        if (address == null) {
            return null;
        }
        if (address instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) address;
            InetAddress inetAddress = isa.getAddress();
            String host = toIp(inetAddress);
            if (inetAddress instanceof Inet6Address) {
                return "[" + host + "]" + ':' + isa.getPort();
            }
            return host + ':' + isa.getPort();
        } else {
            return address.toString();
        }
    }

    /**
     * Get the IP string
     *
     * @param address The address
     * @return The IP string
     */
    public static String toIp(final InetAddress address) {
        return address == null ? null : address.getHostAddress();
        /*
        String result = address == null ? null : address.getHostAddress();
        if (address instanceof Inet6Address) {
            int pos = result.lastIndexOf('%');
            if (pos > 0) {
                return result.substring(0, pos);
            }
        }
        return result;
        */
    }

    /**
     * Get the IP
     *
     * @param address The address
     * @return The IP
     */
    public static String toIp(final InetSocketAddress address) {
        if (address == null) {
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        return inetAddress == null ? address.getHostName() : toIp(inetAddress);
    }

}
