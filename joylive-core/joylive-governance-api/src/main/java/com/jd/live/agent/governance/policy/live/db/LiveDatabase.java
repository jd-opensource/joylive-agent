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
package com.jd.live.agent.governance.policy.live.db;

import com.jd.live.agent.governance.policy.AccessMode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.StringUtils.SEMICOLON_COMMA;
import static com.jd.live.agent.core.util.StringUtils.split;

public class LiveDatabase {
    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private List<String> addresses;

    @Getter
    @Setter
    private String unit;

    @Getter
    @Setter
    private String cell;

    @Getter
    @Setter
    private LiveDatabaseRole role;

    @Getter
    @Setter
    private AccessMode accessMode;

    @Getter
    @Setter
    private transient LiveDatabaseGroup group;

    @Getter
    private transient String primaryAddress;

    @Getter
    private transient Set<String> nodes;

    public LiveDatabase getMaster() {
        if (role == LiveDatabaseRole.MASTER) {
            return this;
        }
        return group == null ? null : group.getMaster(this);
    }

    public boolean contains(String address) {
        return address != null && nodes.contains(address.toLowerCase());
    }

    public boolean contains(String[] shards) {
        if (shards != null) {
            for (String shard : shards) {
                if (nodes.contains(shard.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void cache() {
        if (addresses != null) {
            Set<String> lowerCases = new HashSet<>(addresses.size());
            addresses.forEach(addr -> {
                String[] parts = split(addr, SEMICOLON_COMMA);
                for (String part : parts) {
                    lowerCases.add(part.toLowerCase());
                }
            });
            this.nodes = lowerCases;
            this.primaryAddress = selectAddress();
        }
    }

    private String selectAddress() {
        int size = addresses == null ? 0 : addresses.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return addresses.iterator().next();
        }
        String first = null;
        for (String addr : addresses) {
            if (first == null) {
                first = addr;
            }
            // k8s cluster service address
            if (!addr.contains("svc.cluster.local")) {
                return addr;
            }
        }
        return first;
    }

}
