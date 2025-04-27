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

import java.util.Set;

public class LiveDatabase {
    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private Set<String> addresses;

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

    public LiveDatabase getMaster() {
        if (role == LiveDatabaseRole.MASTER) {
            return this;
        }
        return group == null ? null : group.getMaster(this);
    }

    public boolean contains(String address) {
        return address != null && addresses != null && addresses.contains(address);
    }

    public String getAddress(String address) {
        // TODO get prior address
        return null;
    }

}
