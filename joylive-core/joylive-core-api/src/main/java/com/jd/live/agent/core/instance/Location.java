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
package com.jd.live.agent.core.instance;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the geographical and logical location information for an application or service.
 * This class encapsulates details such as the region, zone, and various identifiers that
 * describe where the application or service is deployed and how it is organized within
 * the infrastructure. This includes identifiers for live space, unit, cell, lane space,
 * and more specific details like cluster, host, and IP address.
 */
@Getter
@Setter
public class Location {

    // The cloud where the application or service is deployed.
    private String cloud;

    // The geographical region where the application or service is deployed.
    private String region;

    // The zone within the region for more granular location specification.
    private String zone;

    // Identifier for the live space, a logical partition within the deployment environment.
    private String liveSpaceId;

    // Represents the planning of live spaces, the composition of unit, cell and traffic distribution
    private String unitRuleId;

    // The unit within the live space, for further logical grouping.
    private String unit;

    // The cell within the unit, another level of logical grouping.
    private String cell;

    // Identifier for the lane space, typically used for routing or organizational purposes.
    private String laneSpaceId;

    // The lane within the lane space, often related to specific functionalities or services.
    private String lane;

    // The cluster where the application or service is running.
    private String cluster;

    // The host machine of the application or service.
    private String host;

    // The IP address of the host machine.
    private String ip;

    public boolean inLiveSpace(String spaceId) {
        if (spaceId == null || spaceId.isEmpty()) {
            return liveSpaceId == null || liveSpaceId.isEmpty();
        } else {
            return spaceId.equals(liveSpaceId);
        }
    }

    public boolean inUnit(String unit) {
        return unit != null && unit.equals(this.unit);
    }

    public boolean inLaneSpace(String spaceId) {
        if (spaceId == null || spaceId.isEmpty()) {
            return laneSpaceId == null || laneSpaceId.isEmpty();
        } else {
            return spaceId.equals(laneSpaceId);
        }
    }

    public boolean inLane(String lane) {
        if (lane == null || lane.isEmpty()) {
            return this.lane == null || this.lane.isEmpty();
        } else {
            return lane.equals(this.lane);
        }
    }

    public boolean inLane(String spaceId, String lane) {
        return inLaneSpace(spaceId) && inLane(lane);
    }
}

