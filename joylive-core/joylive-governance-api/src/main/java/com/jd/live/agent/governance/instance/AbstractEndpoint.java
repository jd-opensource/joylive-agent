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
package com.jd.live.agent.governance.instance;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.governance.request.ServiceRequest;

/**
 * An abstract implementation of the {@link Endpoint} interface that provides caching for
 * various properties. This class uses {@link UnsafeLazyObject} to lazily cache the values
 * of the properties obtained from the {@link Endpoint} interface.
 */
public abstract class AbstractEndpoint extends AbstractAttributes implements Endpoint {

    private String liveSpaceId;

    private String unit;

    private String cell;

    private String laneSpaceId;

    private String lane;

    // Endpoint is request level
    private Integer weight;

    @Override
    public String getLiveSpaceId() {
        if (liveSpaceId == null) {
            liveSpaceId = Endpoint.super.getLiveSpaceId();
            if (liveSpaceId == null) {
                liveSpaceId = "";
            }
        }
        return liveSpaceId;
    }

    @Override
    public String getUnit() {
        if (unit == null) {
            unit = Endpoint.super.getUnit();
            if (unit == null) {
                unit = "";
            }
        }
        return unit;
    }

    @Override
    public String getCell() {
        if (cell == null) {
            cell = Endpoint.super.getCell();
            if (cell == null) {
                cell = "";
            }
        }
        return cell;
    }

    @Override
    public String getLaneSpaceId() {
        if (laneSpaceId == null) {
            laneSpaceId = Endpoint.super.getLaneSpaceId();
            if (laneSpaceId == null) {
                laneSpaceId = "";
            }
        }
        return laneSpaceId;
    }

    @Override
    public String getLane() {
        if (lane == null) {
            lane = Endpoint.super.getLane();
            if (lane == null) {
                lane = "";
            }
        }
        return lane;
    }

    @Override
    public Integer getWeight(ServiceRequest request) {
        if (weight == null) {
            weight = Endpoint.super.getWeight(request);
        }
        return weight;
    }
}
