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

import static com.jd.live.agent.core.util.StringUtils.emptyIfNull;

/**
 * An abstract implementation of the {@link Endpoint} interface that provides caching for
 * various properties. This class uses {@link UnsafeLazyObject} to lazily cache the values
 * of the properties obtained from the {@link Endpoint} interface.
 */
public abstract class AbstractEndpoint extends AbstractAttributes implements Endpoint {

    protected volatile String group;

    protected volatile String liveSpaceId;

    protected volatile String unit;

    protected volatile String cell;

    protected volatile String laneSpaceId;

    protected volatile String lane;

    protected volatile String region;

    protected volatile String zone;

    // adjusted weight
    protected volatile Reweight reweight;

    protected Double weightRatio;

    public AbstractEndpoint() {
    }

    public AbstractEndpoint(String group) {
        this.group = group;
    }

    @Override
    public String getLiveSpaceId() {
        String result = liveSpaceId;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getLiveSpaceId());
            liveSpaceId = result;
        }
        return result;
    }

    @Override
    public String getUnit() {
        String result = unit;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getUnit());
            unit = result;
        }
        return result;
    }

    @Override
    public String getCell() {
        String result = cell;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getCell());
            cell = result;
        }
        return result;
    }

    @Override
    public String getLaneSpaceId() {
        String result = laneSpaceId;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getLaneSpaceId());
            laneSpaceId = result;
        }
        return result;
    }

    @Override
    public String getLane() {
        String result = lane;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getLane());
            lane = result;
        }
        return result;
    }

    @Override
    public String getRegion() {
        String result = region;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getRegion());
            region = result;
        }
        return result;
    }

    @Override
    public String getZone() {
        String result = zone;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getZone());
            zone = result;
        }
        return result;
    }

    @Override
    public String getGroup() {
        String result = group;
        if (result == null) {
            result = emptyIfNull(Endpoint.super.getGroup());
            group = result;
        }
        return result;
    }

    @Override
    public Double getWeightRatio() {
        return weightRatio;
    }

    @Override
    public void setWeightRatio(Double weightRatio) {
        this.weightRatio = weightRatio;
    }

    @Override
    public int reweight(ServiceRequest request) {
        Reweight result = reweight;
        if (result == null) {
            result = new Reweight(getWarmup(), getTimestamp(), getWeight(request));
            reweight = result;
        }
        return result.getWeight(getWeightRatio(), System.currentTimeMillis());
    }

    /**
     * Weight calculator with warmup support and caching.
     */
    private static class Reweight {

        private final int warmup;

        private final long timestamp;

        private final int weight;

        private volatile long lastTime;

        private volatile int lastWeight;

        Reweight(int warmup, long timestamp, int weight) {
            this.warmup = warmup;
            this.timestamp = timestamp;
            this.weight = weight;
        }

        /**
         * Calculates effective weight with ratio applied and caching.
         *
         * @param ratio weight ratio multiplier
         * @param now   current timestamp
         * @return effective weight value
         */
        public int getWeight(final Double ratio, final long now) {
            if (weight <= 0) {
                return 0;
            }
            // first read time, then weight
            long cacheTime = lastTime;
            int cacheWeight = lastWeight;
            if (now - cacheTime < 50) {
                // cache 50ms
                return cacheWeight;
            }
            int value = getWeight(weight, timestamp, warmup, now);
            if (ratio != null) {
                value = (int) (value * ratio);
            }
            value = value < 0 ? 0 : Math.max(1, value);
            cacheWeight = value;
            // first set weight, then time
            lastWeight = cacheWeight;
            lastTime = now;
            return cacheWeight;
        }

        /**
         * Calculates weight based on warmup progression.
         *
         * @param weight    initial weight
         * @param timestamp start time
         * @param duration  warmup duration
         * @param now       current time
         * @return calculated weight
         */
        private int getWeight(int weight, long timestamp, int duration, long now) {
            if (timestamp <= 0 || duration <= 0) {
                return weight;
            }
            long span = now - timestamp;
            if (span <= 0) {
                return -1;
            } else if (span < duration) {
                return (int) (span / ((float) duration / weight));
            }
            return weight;
        }

    }
}
