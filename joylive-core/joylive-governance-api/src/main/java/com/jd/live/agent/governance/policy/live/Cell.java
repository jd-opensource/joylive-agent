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
package com.jd.live.agent.governance.policy.live;

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.policy.AccessMode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.Objects;

@ToString
@Getter
@Setter
public class Cell implements Place {

    private String code;

    private String name;

    private String role;

    private AccessMode accessMode;

    private Map<String, String> labels;

    private final transient LazyObject<String> hostPrefix = new LazyObject<>(() -> code == null ? "" : code.replace('_', '-'));

    public String getLabel(String key) {
        return labels == null ? null : labels.get(key);
    }

    @Override
    public FaultType getPlaceType() {
        return FaultType.CELL;
    }

    public String getHostPrefix() {
        return hostPrefix.get();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Cell)) return false;
        return Objects.equals(code, ((Cell) object).code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
