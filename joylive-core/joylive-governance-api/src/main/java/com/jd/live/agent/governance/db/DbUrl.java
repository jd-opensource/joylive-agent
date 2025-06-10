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

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.StringUtils.split;

@Getter
@AllArgsConstructor
public class DbUrl {

    private String type;

    private String scheme;

    private String schemePart;

    private String user;

    private String password;

    private String userPart;

    private String address;

    private List<Address> nodes;

    private String path;

    private String database;

    private String parameter;

    private String parameterPart;

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        if (schemePart != null && !schemePart.isEmpty()) {
            sb.append(schemePart);
        }
        if (userPart != null && !userPart.isEmpty()) {
            sb.append(userPart);
        }
        if (address != null && !address.isEmpty()) {
            sb.append(address);
        }
        if (path != null && !path.isEmpty()) {
            sb.append(path);
        }
        if (parameterPart != null && !parameterPart.isEmpty()) {
            sb.append(parameterPart);
        }
        return sb.toString();
    }

    public DbUrl address(String address) {
        return new DbUrl(type, scheme, schemePart, user, password, userPart, address, parseNodes(address),
                path, database, parameter, parameterPart);
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

    public boolean hasAddress() {
        return address != null && !address.isEmpty();
    }

    public static DbUrlBuilder builder() {
        return new DbUrlBuilder();
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
        private List<Address> nodes;
        private String path;
        private String database;
        private String parameter;
        private String parameterPart;

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

        public DbUrlBuilder parameter(String parameter) {
            this.parameter = parameter;
            return this;
        }

        public DbUrlBuilder parameterPart(String parameterPart) {
            this.parameterPart = parameterPart;
            return this;
        }

        public DbUrl build() {
            return new DbUrl(type, scheme, schemePart, user, password, userPart, address, nodes, path, database, parameter, parameterPart);
        }
    }

}
