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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.map.CaseInsensitiveSet;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.policy.AccessMode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.Constants.PREDICATE_K8S_SERVICE;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.splitList;
import static com.jd.live.agent.core.util.URI.getAddress;

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
    private transient LiveDatabaseGroup group;

    @Getter
    private transient String primaryAddress;

    // CaseInsensitiveSet
    @Getter
    private transient Set<String> nodes;

    // @JacksonIgnore for getWriteDatabase
    private transient LiveDatabase writeDatabase;

    protected void setGroup(LiveDatabaseGroup group) {
        this.group = group;
    }

    public LiveDatabase getWriteDatabase() {
        if (role == LiveDatabaseRole.MASTER) {
            return this;
        }
        return group == null ? null : group.getWriteDatabase(this);
    }

    public LiveDatabase getReadDatabase(String unit, String cell) {
        return group == null ? null : group.getReadDatabase(this, unit, cell);
    }

    public boolean contains(String address) {
        return address != null && nodes.contains(address);
    }

    public boolean contains(String[] shards) {
        if (shards != null) {
            for (String shard : shards) {
                if (nodes.contains(shard)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void cache() {
        if (addresses != null) {
            List<String> lowerAddresses = new ArrayList<>(addresses.size());
            Set<String> lowerNodes = new CaseInsensitiveSet(addresses.size());
            addresses.forEach(addr -> {
                String lowerAddr = addr.toLowerCase();
                lowerAddresses.add(lowerAddr);
                List<URI> uris = toList(splitList(lowerAddr), URI::parse);
                for (URI uri : uris) {
                    String host = uri.getHost();
                    Integer port = uri.getPort();
                    // for development environment
                    if (Ipv4.isLocalHost(host)) {
                        Ipv4.LOCAL_HOST.forEach(h -> lowerNodes.add(getAddress(h, port)));
                    } else {
                        lowerNodes.add(uri.getAddress());
                    }
                }
            });
            this.addresses = lowerAddresses;
            this.nodes = lowerNodes;
            this.primaryAddress = selectAddress();
        }
    }

    public boolean isLocation(String unit, String cell) {
        return isLocationEqual(this.unit, unit) && isLocationEqual(this.cell, cell);
    }

    private String selectAddress() {
        int size = addresses == null ? 0 : addresses.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return addresses.get(0);
        }
        String first = null;
        for (String addr : addresses) {
            if (first == null) {
                first = addr;
            }
            // predicate k8s cluster service name
            if (!PREDICATE_K8S_SERVICE.test(addr)) {
                return addr;
            }
        }
        return first;
    }

    private boolean isLocationEqual(String source, String target) {
        if (source == null || source.isEmpty()) {
            return target == null || target.isEmpty();
        }
        return source.equals(target);
    }

}
