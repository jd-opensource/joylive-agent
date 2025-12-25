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
package com.jd.live.agent.core.bootstrap.env.location;

import com.jd.live.agent.core.bootstrap.AppEnv;
import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.network.IpLong;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.core.util.network.Segment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.splitList;
import static com.jd.live.agent.core.util.StringUtils.splitMap;

/**
 * Supplies location information based on IP address configuration.
 * Determines region, zone, unit, and cell locations by matching local IP against configured IP segments.
 */
@Injectable
@Extension(value = "LocationIPSupplier", order = EnvSupplier.ORDER_LOCATION_IP_SUPPLIER)
public class LocationIPSupplier implements EnvSupplier {

    @Override
    public void process(AppEnv env) {
        IpLong ip = new IpLong(Ipv4.getLocalIp());
        process(env, ip, "APPLICATION_LOCATION_REGION", env.getString("CONFIG_IP_REGION"));
        process(env, ip, "APPLICATION_LOCATION_ZONE", env.getString("CONFIG_IP_ZONE"));
        process(env, ip, "APPLICATION_LOCATION_UNIT", env.getString("CONFIG_IP_UNIT"));
        process(env, ip, "APPLICATION_LOCATION_CELL", env.getString("CONFIG_IP_CELL"));
    }

    /**
     * Processes location configuration for the specified environment variable.
     * Matches local IP against configured segments to determine location.
     */
    private void process(AppEnv env, IpLong ip, String envName, String value) {
        String location = (String) env.get(envName);
        if (location != null && !location.isEmpty()) {
            return;
        }
        Map<String, List<String>> map = new HashMap<>();
        splitMap(value, o -> o == ';', true, (k, v) -> {
            map.put(k, splitList(v, ','));
            return true;
        });
        if (map.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String name = entry.getKey();
            List<String> segments = entry.getValue();
            for (String segment : segments) {
                if (Segment.parse(segment).contains(ip)) {
                    env.put(envName, name);
                    return;
                }
            }
        }
    }
}
