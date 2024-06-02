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

import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
@Setter
public class LiveTransmission {
    public static final String X_LIVE_SPACE_ID = "x-live-space-id";
    public static final String X_LIVE_RULE_ID = "x-live-rule-id";
    public static final String X_LIVE_UID = "x-live-uid";
    public static final String X_LANE_SPACE_ID = "x-lane-space-id";
    public static final String X_LANE_CODE = "x-lane-code";
    private String carrier;
    private String liveSpaceId;
    private String ruleId;
    private String uid;
    private String laneSpaceId;
    private String lane;

    public LiveTransmission(String carrier, String liveSpaceId, String ruleId, String uid, String laneSpaceId, String lane) {
        this.carrier = carrier;
        this.liveSpaceId = liveSpaceId;
        this.ruleId = ruleId;
        this.uid = uid;
        this.laneSpaceId = laneSpaceId;
        this.lane = lane;
    }

    public LiveTransmission(String carrier, Function<String, String> tagFunc) {
        this.carrier = carrier;
        liveSpaceId = tagFunc.apply(X_LIVE_SPACE_ID);
        ruleId = tagFunc.apply(X_LIVE_RULE_ID);
        uid = tagFunc.apply(X_LIVE_UID);
        laneSpaceId = tagFunc.apply(X_LANE_SPACE_ID);
        lane = tagFunc.apply(X_LANE_CODE);
    }

    @Override
    public String toString() {
        return carrier + "{" +
                "x-live-space-id=" + (liveSpaceId == null ? "null" : ('\'' + liveSpaceId + '\'')) +
                ", x-live-rule-id=" + (ruleId == null ? "null" : ('\'' + ruleId + '\'')) +
                ", x-live-uid=" + (uid == null ? "null" : ('\'' + uid + '\'')) +
                ", x-lane-space-id=" + (laneSpaceId == null ? "null" : ('\'' + laneSpaceId + '\'')) +
                ", x-lane-code=" + (lane == null ? "null" : ('\'' + lane + '\'')) +
                '}';
    }
}
