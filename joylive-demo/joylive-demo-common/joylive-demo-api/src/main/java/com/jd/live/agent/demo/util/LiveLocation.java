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
package com.jd.live.agent.demo.util;

import com.jd.live.agent.core.util.network.Ipv4;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveLocation {
    private String liveSpaceId;
    private String unit;
    private String cell;
    private String laneSpaceId;
    private String lane;
    private String cluster;
    private String cloud;
    private String group;
    private String ip;

    public LiveLocation() {
        liveSpaceId = System.getProperty("x-live-space-id");
        unit = System.getProperty("x-live-unit");
        cell = System.getProperty("x-live-cell");
        laneSpaceId = System.getProperty("x-lane-space-id");
        lane = System.getProperty("x-lane-code");
        cluster = System.getProperty("x-live-cluster");
        cloud = System.getProperty("x-live-cloud");
        group = System.getProperty("x-service-group");
        ip = Ipv4.getLocalIp();
    }

    @Override
    public String toString() {
        return "Location{" +
                "live-space-id=" + (liveSpaceId == null ? "null" : ('\'' + liveSpaceId + '\'')) +
                ", unit=" + (unit == null ? "null" : ('\'' + unit + '\'')) +
                ", cell=" + (cell == null ? "null" : ('\'' + cell + '\'')) +
                ", lane-space-id=" + (laneSpaceId == null ? "null" : ('\'' + laneSpaceId + '\'')) +
                ", lane=" + (lane == null ? "null" : ('\'' + lane + '\'')) +
                ", cluster=" + (cluster == null ? "null" : ('\'' + cluster + '\'')) +
                ", cloud=" + (cloud == null ? "null" : ('\'' + cloud + '\'')) +
                ", group=" + (group == null ? "null" : ('\'' + group + '\'')) +
                ", ip=" + (ip == null ? "null" : ('\'' + ip + '\'')) +
                '}';
    }
}
