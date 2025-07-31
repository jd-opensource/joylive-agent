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
package com.jd.live.agent.implement.event.opentelemetry;

import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.ExporterConfig;
import io.opentelemetry.api.metrics.Meter;

/**
 * Factory interface for creating metric subscriptions with OpenTelemetry integration.
 * Provides extensible metric creation capabilities for different exporter configurations.
 *
 * @param <T> the type of metric data handled by the subscription
 */
@Extensible("MetricFactory")
public interface MetricFactory<T> extends AutoCloseable {

    /**
     * Creates a metric subscription with the specified configuration and components.
     *
     * @param config      the exporter configuration
     * @param application the application instance
     * @param meter       the OpenTelemetry meter for metric collection
     * @return a subscription for handling metric data of type T
     */
    Subscription<T> create(ExporterConfig config, Application application, Meter meter);

    @Override
    default void close() {

    }
}
