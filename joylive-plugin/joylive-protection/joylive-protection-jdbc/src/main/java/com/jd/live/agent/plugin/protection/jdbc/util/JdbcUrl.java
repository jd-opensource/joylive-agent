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
package com.jd.live.agent.plugin.protection.jdbc.util;

import com.jd.live.agent.core.util.network.Address;
import lombok.Getter;

@Getter
public class JdbcUrl {

    private final String scheme;

    private final String user;

    private final String password;

    private final String host;

    private final String uriHost;

    private final Integer port;

    private final String address;

    private final String path;

    private final String query;

    public JdbcUrl(String scheme, String user, String password, Address address, String path, String query) {
        this.scheme = scheme;
        this.user = user;
        this.password = password;
        this.host = address.getHost();
        this.uriHost = address.getUriHost();
        this.port = address.getPort();
        this.address = address.getAddress();
        this.path = path;
        this.query = query;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        if (scheme != null && !scheme.isEmpty()) {
            sb.append(scheme).append("://");
        }
        int userLen = user == null ? 0 : user.length();
        int passwordLen = password == null ? 0 : password.length();
        if (userLen > 0) {
            sb.append(user);
        }
        if (passwordLen > 0) {
            sb.append(':').append(password);
        }
        if (userLen > 0 || passwordLen > 0) {
            sb.append('@');
        }
        if (host != null) {
            sb.append(host);
        }
        if (port != null) {
            sb.append(':').append(port);
        }
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("/")) {
                sb.append(path);
            } else {
                sb.append('/').append(path);
            }
        }
        if (query != null && !query.isEmpty()) {
            sb.append('?').append(query);
        }
        return sb.toString();
    }

    public static JdbcUrl parse(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String query = null;
        int pos = url.indexOf('?');
        if (pos >= 0) {
            query = url.substring(pos + 1);
            url = url.substring(0, pos);
        }
        String scheme = null;
        pos = url.indexOf("://");
        if (pos >= 0) {
            scheme = url.substring(0, pos);
            url = url.substring(pos + 3);
        }
        String path = null;
        pos = url.indexOf('/');
        if (pos >= 0) {
            path = url.substring(pos);
            url = url.substring(0, pos);
        }
        String host = null;
        String secure = null;
        pos = url.indexOf('@');
        if (pos >= 0) {
            host = url.substring(pos + 1);
            secure = url.substring(0, pos);
        } else {
            host = url;
            secure = "";
        }
        String user = secure.isEmpty() ? null : url;
        String password = null;
        pos = secure.indexOf(':');
        if (pos >= 0) {
            user = secure.substring(0, pos);
            password = secure.substring(pos + 1);
        }

        Address address = Address.parse(host);
        return new JdbcUrl(scheme, user, password, address, path, query);
    }
}
