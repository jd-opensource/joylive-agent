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
package com.jd.live.agent.implement.event.opentelemetry.exporter;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LoggingMetricExporter implements MetricExporter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricExporter.class);

    private final AtomicBoolean started = new AtomicBoolean(true);

    private final AggregationTemporality aggregationTemporality;

    /**
     * Returns a new {@link LoggingMetricExporter} with an aggregation temporality of
     * {@link AggregationTemporality#CUMULATIVE}.
     */
    public static LoggingMetricExporter create() {
        return create(AggregationTemporality.CUMULATIVE);
    }

    /**
     * Returns a new {@link LoggingMetricExporter} with the given {@code aggregationTemporality}.
     */
    public static LoggingMetricExporter create(AggregationTemporality aggregationTemporality) {
        return new LoggingMetricExporter(aggregationTemporality);
    }

    private LoggingMetricExporter(AggregationTemporality aggregationTemporality) {
        this.aggregationTemporality = aggregationTemporality;
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return aggregationTemporality;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (!started.get()) {
            return CompletableResultCode.ofFailure();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Received a collection of {} metrics for export.", metrics.size());
            for (MetricData metric : metrics) {
                logger.debug("metric: {}", metric);
            }
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        started.set(false);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public String toString() {
        return "LoggingMetricExporter";
    }
}