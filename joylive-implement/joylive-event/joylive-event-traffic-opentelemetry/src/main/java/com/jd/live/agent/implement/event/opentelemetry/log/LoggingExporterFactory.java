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
package com.jd.live.agent.implement.event.opentelemetry.log;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.implement.event.opentelemetry.ExporterFactory;
import com.jd.live.agent.implement.event.opentelemetry.config.CounterConfig;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

import java.time.Duration;

@Extension("logging")
public class LoggingExporterFactory implements ExporterFactory {

    @Override
    public MetricReader create(CounterConfig config) {
        MetricExporter exporter = LoggingMetricExporter.create(AggregationTemporality.DELTA);
        return PeriodicMetricReader.builder(exporter).setInterval(Duration.ofMillis(config.getReaderInterval())).build();
    }
}
