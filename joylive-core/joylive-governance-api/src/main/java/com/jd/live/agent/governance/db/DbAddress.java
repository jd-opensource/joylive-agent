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

import lombok.Getter;

import java.util.Objects;

import static com.jd.live.agent.core.util.StringUtils.split;

/**
 * Represents a database address.
 */
@Getter
public class DbAddress {

    public static final String TYPE_DB = "DB";

    private final String type;

    private final String address;

    private final String[] nodes;

    public DbAddress(String address) {
        this(TYPE_DB, address);
    }

    public DbAddress(String type, String address) {
        this.type = type;
        this.address = address;
        this.nodes = split(address);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DbAddress)) return false;
        DbAddress that = (DbAddress) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }

    @Override
    public String toString() {
        return address;
    }
}
