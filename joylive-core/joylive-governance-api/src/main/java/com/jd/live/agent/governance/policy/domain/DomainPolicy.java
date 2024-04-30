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
package com.jd.live.agent.governance.policy.domain;

import com.jd.live.agent.governance.policy.lane.LaneDomain;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.LiveDomain;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.UnitDomain;
import lombok.Getter;

@Getter
public class DomainPolicy {

    private final LiveSpace liveSpace;

    private final LiveDomain liveDomain;

    private final UnitDomain unitDomain;

    private final boolean unit;

    private final LaneSpace laneSpace;

    private final LaneDomain laneDomain;

    public DomainPolicy(LaneSpace laneSpace, LaneDomain laneDomain) {
        this(null, null, null, laneSpace, laneDomain);
    }

    public DomainPolicy(LiveSpace liveSpace, LiveDomain liveDomain) {
        this(liveSpace, liveDomain, null, null, null);
    }

    public DomainPolicy(LiveSpace liveSpace, LiveDomain liveDomain, UnitDomain unitDomain) {
        this(liveSpace, liveDomain, unitDomain, null, null);
    }

    public DomainPolicy(LiveSpace liveSpace, LiveDomain liveDomain, UnitDomain unitDomain, LaneSpace laneSpace, LaneDomain laneDomain) {
        this.liveSpace = liveSpace;
        this.liveDomain = liveDomain;
        this.unitDomain = unitDomain;
        this.unit = unitDomain != null && liveDomain != null && !liveDomain.getHost().equals(unitDomain.getHost());
        this.laneSpace = laneSpace;
        this.laneDomain = laneDomain;
    }

    public boolean isUnit() {
        return unit;
    }

    public String getUnit() {
        return unitDomain == null ? null : unitDomain.getUnit();
    }

    public String getBackend() {
        if (!unit) {
            return liveDomain == null ? null : liveDomain.getBackend();
        }
        return unitDomain == null ? null : unitDomain.getBackend();
    }

}
