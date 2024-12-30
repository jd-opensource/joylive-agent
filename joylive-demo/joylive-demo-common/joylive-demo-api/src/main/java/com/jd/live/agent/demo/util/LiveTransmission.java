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

import java.util.function.Function;

import static com.jd.live.agent.demo.response.LiveTransmission.X_LIVE_CLOUD;
import static com.jd.live.agent.demo.response.LiveTransmission.X_LIVE_CLUSTER;

@Getter
@Setter
public class LiveTransmission {
    public static final String X_LIVE_SPACE_ID = "x-live-space-id";
    public static final String X_LIVE_RULE_ID = "x-live-rule-id";
    public static final String X_LIVE_UID = "x-live-uid";
    public static final String X_LANE_SPACE_ID = "x-lane-space-id";
    public static final String X_LANE_CODE = "x-lane-code";
    public static final String X_SERVICE_GROUP = "x-service-group";
    private String carrier;
    private String liveSpaceId;
    private String ruleId;
    private String uid;
    private String laneSpaceId;
    private String lane;
    private String cluster;
    private String cloud;
    private String group;
    private String ip;

    public LiveTransmission(String carrier, String liveSpaceId, String ruleId, String uid, String laneSpaceId, String lane, String cluster, String cloud, String group) {
        this.carrier = carrier;
        this.liveSpaceId = liveSpaceId;
        this.ruleId = ruleId;
        this.uid = uid;
        this.laneSpaceId = laneSpaceId;
        this.lane = lane;
        this.cluster = cluster;
        this.cloud = cloud;
        this.group = group;
        this.ip = Ipv4.getLocalIp();
    }

    public LiveTransmission(String carrier, Function<String, String> tagFunc) {
        this.carrier = carrier;
        liveSpaceId = tagFunc.apply(X_LIVE_SPACE_ID);
        ruleId = tagFunc.apply(X_LIVE_RULE_ID);
        uid = tagFunc.apply(X_LIVE_UID);
        laneSpaceId = tagFunc.apply(X_LANE_SPACE_ID);
        lane = tagFunc.apply(X_LANE_CODE);
        cluster = tagFunc.apply(X_LIVE_CLUSTER);
        cloud = tagFunc.apply(X_LIVE_CLOUD);
        group = tagFunc.apply(X_SERVICE_GROUP);
    }

    @Override
    public String toString() {
        return carrier + "{" +
                "x-live-space-id=" + (liveSpaceId == null ? "null" : ('\'' + liveSpaceId + '\'')) +
                ", x-live-rule-id=" + (ruleId == null ? "null" : ('\'' + ruleId + '\'')) +
                ", x-live-uid=" + (uid == null ? "null" : ('\'' + uid + '\'')) +
                ", x-lane-space-id=" + (laneSpaceId == null ? "null" : ('\'' + laneSpaceId + '\'')) +
                ", x-lane-code=" + (lane == null ? "null" : ('\'' + lane + '\'')) +
                ", x-live-cluster=" + (cluster == null ? "null" : ('\'' + cluster + '\'')) +
                ", x-live-cloud=" + (cloud == null ? "null" : ('\'' + cloud + '\'')) +
                ", x-service-group=" + (group == null ? "null" : ('\'' + group + '\'')) +
                '}';
    }
}
