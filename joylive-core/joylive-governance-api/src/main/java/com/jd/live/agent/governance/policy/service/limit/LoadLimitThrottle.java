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
package com.jd.live.agent.governance.policy.service.limit;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoadLimitThrottle extends LoadMetric implements Comparable<LoadLimitThrottle> {

    private int ratio;

    public LoadLimitThrottle() {
    }

    public LoadLimitThrottle(Integer cpuUsage, Integer loadUsage, int ratio) {
        super(cpuUsage, loadUsage);
        this.ratio = ratio;
    }

    @Override
    public int compareTo(LoadLimitThrottle o) {
        return Integer.compare(o.ratio, ratio);
    }

    public static int getRatio(LoadMetric metric, List<LoadLimitThrottle> throttles) {
        if (metric != null && throttles != null && !throttles.isEmpty()) {
            for (LoadLimitThrottle throttle : throttles) {
                if (throttle.match(metric)) {
                    return throttle.getRatio();
                }
            }
        }
        return 0;
    }
}
