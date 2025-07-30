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
package com.jd.live.agent.implement.event.opentelemetry.subscription;

import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.ExporterConfig;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.implement.event.opentelemetry.MetricFactory;
import io.opentelemetry.api.metrics.Meter;

@Extension("TrafficMetric")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_EXPORTER_TRAFFIC_ENABLED, matchIfMissing = true)
public class TrafficMetricFactory implements MetricFactory<TrafficEvent> {

    @Override
    public Subscription<TrafficEvent> create(ExporterConfig config, Application application, Meter meter) {
        return new TrafficMetric(config.getTrafficConfig(), application, meter);
    }
}
