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
package com.jd.live.agent.governance.util;

import com.jd.live.agent.core.util.Close;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * A network socket detector that tests connectivity to a given address.
 * Implements {@link Predicate} for address validation.
 */
public class SocketDetector implements BiPredicate<String, Integer> {

    private final int connectTimeout;

    private final int defaultPort;

    private final SocketListener listener;

    public SocketDetector(SocketListener listener) {
        this(1000, 80, listener);
    }

    public SocketDetector(int connectTimeout, int defaultPort, SocketListener listener) {
        this.connectTimeout = connectTimeout;
        this.defaultPort = defaultPort;
        this.listener = listener;
    }

    @Override
    public boolean test(String host, Integer port) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        Socket socket = null;
        OutputStream out = null;
        InputStream in = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port == null || port < 0 || port > 65535 ? defaultPort : port), connectTimeout);
            out = socket.getOutputStream();
            listener.send(out);
            in = socket.getInputStream();
            return listener.receive(in);
        } catch (Throwable e) {
            return false;
        } finally {
            Close.instance().close(socket, out, in);
        }
    }

    /**
     * Interface for customizing socket communication behavior.
     */
    public interface SocketListener {

        /**
         * Sends data through the established connection.
         *
         * @param out the output stream to write to
         * @throws IOException if communication fails
         */
        void send(OutputStream out) throws IOException;

        /**
         * Receives and validates response from the server.
         *
         * @param in the input stream to read from
         * @return true if response is valid, false otherwise
         * @throws IOException if communication fails
         */
        boolean receive(InputStream in) throws IOException;

    }

}
