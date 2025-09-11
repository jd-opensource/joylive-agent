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

import com.jd.live.agent.core.event.*;
import com.jd.live.agent.core.event.AgentEvent.EventType;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.config.ExporterConfig;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.implement.event.opentelemetry.exporter.LoggingExporterFactory;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable
@Injectable
@Extension("OpenTelemetryExporter")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_EXPORTER_ENABLED, matchIfMissing = true)
public class OpenTelemetryExporter implements Subscriber {

    private static final String LIVE_SCOPE = "com.jd.live";
    private static final String SERVICE_NAME = "service.name";

    @Config(ExporterConfig.CONFIG_EXPORTER)
    private ExporterConfig config;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(value = EventBus.COMPONENT_EVENT_BUS, component = true)
    private EventBus eventBus;

    @Inject
    private Map<String, ExporterFactory> exporterFactories;

    @Inject
    private List<MetricFactory<?>> metricFactories;

    private OpenTelemetrySdk sdk;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public Subscription<?>[] subscribe() {
        return new Subscription[]{new SystemSubscription()};
    }

    @Override
    public void close() {
        Close.instance().close(sdk).close(metricFactories);
    }

    private class SystemSubscription implements Subscription<AgentEvent> {

        @Override
        public String getTopic() {
            return Publisher.SYSTEM;
        }

        @Override
        public void handle(List<Event<AgentEvent>> events) {
            for (Event<AgentEvent> event : events) {
                if (event.getData().getType() == EventType.APPLICATION_READY) {
                    // delay initialization until application is ready to avoid lock contention with other agent.
                    if (initialized.compareAndSet(false, true)) {
                        Resource resource = Resource.getDefault().toBuilder().put(SERVICE_NAME, application.getName()).build();
                        ExporterFactory factory = config.getType() == null ? null : exporterFactories.get(config.getType());
                        factory = factory == null ? new LoggingExporterFactory() : factory;
                        MetricReader reader = factory.create(config);

                        SdkMeterProvider provider = SdkMeterProvider.builder().setResource(resource).registerMetricReader(reader).build();
                        sdk = OpenTelemetrySdk.builder().setMeterProvider(provider).buildAndRegisterGlobal();
                        if (metricFactories != null) {
                            Meter meter = sdk.getMeter(LIVE_SCOPE);
                            for (MetricFactory<?> metricFactory : metricFactories) {
                                eventBus.subscribe(metricFactory.create(config, application, meter));
                            }
                        }
                    }
                }
            }
        }
    }
}
