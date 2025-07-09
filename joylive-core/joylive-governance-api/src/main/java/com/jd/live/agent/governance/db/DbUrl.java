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
package com.jd.live.agent.governance.db;

import com.jd.live.agent.core.util.network.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.core.util.StringUtils.splitList;

@AllArgsConstructor
public class DbUrl {

    @Getter
    private String type;

    @Getter
    private String scheme;

    private String schemePart;

    @Getter
    private String user;

    @Getter
    private String password;

    private String userPart;

    @Getter
    private String address;

    private String addressPart;

    private DbUrlAddressUpdater addressUpdater;

    @Getter
    private List<Address> nodes;

    @Getter
    private String path;

    @Getter
    private String database;

    private char parameterBeginDelimiter;

    private char parameterDelimiter;

    private Map<String, String> parameters;

    private String parameterPart;

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        if (schemePart != null && !schemePart.isEmpty()) {
            sb.append(schemePart);
        }
        if (userPart != null && !userPart.isEmpty()) {
            sb.append(userPart);
        }
        if (addressPart != null) {
            // addressPart maybe empty.
            sb.append(addressPart);
        } else if (address != null && !address.isEmpty()) {
            sb.append(address);
        }
        if (path != null && !path.isEmpty()) {
            sb.append(path);
        }
        if (parameterPart != null && !parameterPart.isEmpty()) {
            sb.append(parameterPart);
        } else if (parameters != null && !parameters.isEmpty()) {
            sb.append(parameterBeginDelimiter == 0 ? '?' : parameterBeginDelimiter);
            int count = 0;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (count++ > 0) {
                    sb.append(parameterDelimiter == 0 ? '&' : parameterDelimiter);
                }
                sb.append(entry.getKey());
                if (entry.getValue() != null) {
                    sb.append("=").append(entry.getValue());
                }
            }
        }
        return sb.toString();
    }

    public DbUrl address(String address) {
        DbUrl.DbUrlBuilder builder = DbUrl.builder(this);
        if (addressUpdater != null) {
            addressUpdater.update(address, builder);
            builder.nodes(getList(address));
        } else {
            builder.address(address).nodes(getList(address));
        }
        return builder.build();
    }

    public static List<Address> parseNodes(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        String[] hosts = split(address, ',');
        List<Address> nodes = new ArrayList<>(hosts.length);
        for (String host : hosts) {
            nodes.add(Address.parse(host));
        }
        return nodes;
    }

    public String getParameter(String key) {
        return key == null || parameters == null ? null : parameters.get(key);
    }

    public Map<String, String> getParameters() {
        return parameters == null ? null : Collections.unmodifiableMap(parameters);
    }

    public boolean hasAddress() {
        return address != null && !address.isEmpty();
    }

    public static DbUrlBuilder builder() {
        return new DbUrlBuilder();
    }

    public static DbUrlBuilder builder(DbUrl url) {
        return new DbUrlBuilder(url);
    }

    public static List<Address> getList(String address) {
        return toList(splitList(address), Address::parse);
    }

    @Getter
    public static class DbUrlBuilder {
        private String type;
        private String scheme;
        private String schemePart;
        private String user;
        private String password;
        private String userPart;
        private String address;
        private String addressPart;
        private DbUrlAddressUpdater addressUpdater;
        private List<Address> nodes;
        private String path;
        private String database;
        private char parameterBeginDelimiter;
        private char parameterDelimiter;
        private Map<String, String> parameters;
        private Map<String, String> caseInsensitiveParameters;
        private String parameterPart;

        public DbUrlBuilder() {
        }

        public DbUrlBuilder(DbUrl url) {
            this.type = url.type;
            this.scheme = url.scheme;
            this.schemePart = url.schemePart;
            this.user = url.user;
            this.password = url.password;
            this.userPart = url.userPart;
            this.address = url.address;
            this.addressPart = url.addressPart;
            this.addressUpdater = url.addressUpdater;
            this.nodes = url.nodes;
            this.path = url.path;
            this.database = url.database;
            this.parameterBeginDelimiter = url.parameterBeginDelimiter;
            this.parameterDelimiter = url.parameterDelimiter;
            this.parameters = url.parameters;
            this.parameterPart = url.parameterPart;
        }

        public DbUrlBuilder type(String type) {
            this.type = type;
            return this;
        }

        public DbUrlBuilder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public DbUrlBuilder schemePart(String schemePart) {
            this.schemePart = schemePart;
            return this;
        }

        public DbUrlBuilder user(String user) {
            this.user = user;
            return this;
        }

        public DbUrlBuilder password(String password) {
            this.password = password;
            return this;
        }

        public DbUrlBuilder userPart(String userPart) {
            this.userPart = userPart;
            return this;
        }

        public DbUrlBuilder address(String address) {
            this.address = address;
            return this;
        }

        public DbUrlBuilder addressPart(String addressPart) {
            this.addressPart = addressPart;
            return this;
        }

        public DbUrlBuilder addressUpdater(DbUrlAddressUpdater addressUpdater) {
            this.addressUpdater = addressUpdater;
            return this;
        }

        public DbUrlBuilder nodes(List<Address> nodes) {
            this.nodes = nodes;
            return this;
        }

        public DbUrlBuilder path(String path) {
            this.path = path;
            return this;
        }

        public DbUrlBuilder database(String database) {
            this.database = database;
            return this;
        }

        public DbUrlBuilder parameterBeginDelimiter(char parameterBeginDelimiter) {
            this.parameterBeginDelimiter = parameterBeginDelimiter;
            return this;
        }

        public DbUrlBuilder parameterDelimiter(char parameterDelimiter) {
            this.parameterDelimiter = parameterDelimiter;
            return this;
        }

        public DbUrlBuilder parameter(String key, String value) {
            if (key == null || key.isEmpty()) {
                return this;
            }
            if (parameters == null) {
                parameters = new LinkedHashMap<>();
            }
            caseInsensitiveParameters = null;
            parameters.put(key, value);
            return this;
        }

        public DbUrlBuilder parameters(Map<String, String> parameters) {
            this.parameters = parameters;
            this.caseInsensitiveParameters = null;
            return this;
        }

        public DbUrlBuilder parameterPart(String parameterPart) {
            this.parameterPart = parameterPart;
            return this;
        }

        public String getParameter(String name) {
            if (name == null || parameters == null) {
                return null;
            }
            if (caseInsensitiveParameters == null) {
                caseInsensitiveParameters = new HashMap<>();
                parameters.forEach((key, value) -> caseInsensitiveParameters.put(key.toLowerCase(), value));
            }
            return caseInsensitiveParameters.get(name.toLowerCase());
        }

        public DbUrl build() {
            return new DbUrl(type, scheme, schemePart,
                    user, password, userPart,
                    address, addressPart, addressUpdater, nodes,
                    path, database, parameterBeginDelimiter, parameterDelimiter, parameters, parameterPart);
        }
    }

}
