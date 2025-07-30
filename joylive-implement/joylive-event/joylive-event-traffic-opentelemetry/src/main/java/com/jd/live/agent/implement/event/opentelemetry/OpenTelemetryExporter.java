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

import com.jd.live.agent.core.event.Subscriber;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.ExtensionInitializer;
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

@Configurable
@Injectable
@Extension("OpenTelemetryExporter")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_EXPORTER_ENABLED, matchIfMissing = true)
public class OpenTelemetryExporter implements Subscriber, ExtensionInitializer {

    private static final String LIVE_SCOPE = "com.jd.live";
    private static final String SERVICE_NAME = "service.name";

    @Config(ExporterConfig.CONFIG_EXPORTER)
    private ExporterConfig config;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject
    private Map<String, ExporterFactory> exporterFactories;

    @Inject
    private List<MetricFactory<?>> metricFactories;

    private OpenTelemetrySdk sdk;

    @Override
    public void initialize() {
        Resource resource = Resource.getDefault().toBuilder().put(SERVICE_NAME, application.getName()).build();
        ExporterFactory factory = config.getType() == null ? null : exporterFactories.get(config.getType());
        factory = factory == null ? new LoggingExporterFactory() : factory;
        MetricReader reader = factory.create(config);

        SdkMeterProvider provider = SdkMeterProvider.builder().setResource(resource).registerMetricReader(reader).build();
        sdk = OpenTelemetrySdk.builder().setMeterProvider(provider).buildAndRegisterGlobal();
    }

    @Override
    public Subscription<?>[] subscribe() {
        if (metricFactories == null || metricFactories.isEmpty()) {
            return new Subscription[0];
        }
        Meter meter = sdk.getMeter(LIVE_SCOPE);
        Subscription<?>[] subscriptions = new Subscription[metricFactories.size()];
        int i = 0;
        for (MetricFactory<?> factory : metricFactories) {
            subscriptions[i++] = factory.create(config, application, meter);
        }
        return subscriptions;
    }

    @Override
    public void close() {
        Close.instance().close(sdk).close(metricFactories);
    }

}
