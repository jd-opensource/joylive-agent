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

import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.implement.event.opentelemetry.config.CounterConfig;
import com.jd.live.agent.implement.event.opentelemetry.config.ExporterConfig;
import com.jd.live.agent.implement.event.opentelemetry.log.LoggingExporterFactory;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;

import java.util.List;
import java.util.Map;

import static com.jd.live.agent.governance.event.TrafficEvent.*;

@Configurable
@Injectable
@Extension("EventExporter")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_COUNTER_ENABLED, matchIfMissing = true)
public class EventExporter implements Subscription<TrafficEvent>, ExtensionInitializer {

    private static final String LIVE_SCOPE = "com.jd.live";

    private static final String REQUESTS = "requests";

    private static final AttributeKey<String> ATTRIBUTE_APPLICATION = AttributeKey.stringKey(KEY_APPLICATION);

    private static final AttributeKey<String> ATTRIBUTE_LIVE_SPACE_ID = AttributeKey.stringKey(KEY_LIVE_SPACE_ID);

    private static final AttributeKey<String> ATTRIBUTE_LIVE_RULE_ID = AttributeKey.stringKey(KEY_LIVE_RULE_ID);

    private static final AttributeKey<String> ATTRIBUTE_LOCAL_UNIT = AttributeKey.stringKey(KEY_LOCAL_UNIT);

    private static final AttributeKey<String> ATTRIBUTE_LOCAL_CELL = AttributeKey.stringKey(KEY_LOCAL_CELL);

    private static final AttributeKey<String> ATTRIBUTE_LOCAL_IP = AttributeKey.stringKey(KEY_LOCAL_IP);

    private static final AttributeKey<String> ATTRIBUTE_TARGET_UNIT = AttributeKey.stringKey(KEY_TARGET_UNIT);

    private static final AttributeKey<String> ATTRIBUTE_TARGET_CELL = AttributeKey.stringKey(KEY_TARGET_CELL);

    private static final AttributeKey<String> ATTRIBUTE_LIVE_DOMAIN = AttributeKey.stringKey(KEY_LIVE_DOMAIN);

    private static final AttributeKey<String> ATTRIBUTE_LIVE_PATH = AttributeKey.stringKey(KEY_LIVE_PATH);

    private static final AttributeKey<String> ATTRIBUTE_LIVE_VARIABLE = AttributeKey.stringKey(KEY_LIVE_VARIABLE);

    private static final AttributeKey<String> ATTRIBUTE_LIVE_BIZ_VARIABLE = AttributeKey.stringKey(KEY_LIVE_BIZ_VARIABLE);

    private static final AttributeKey<String> ATTRIBUTE_LANE_SPACE_ID = AttributeKey.stringKey(KEY_LANE_SPACE_ID);

    private static final AttributeKey<String> ATTRIBUTE_LANE_RULE_ID = AttributeKey.stringKey(KEY_LANE_RULE_ID);

    private static final AttributeKey<String> ATTRIBUTE_LOCAL_LANE = AttributeKey.stringKey(KEY_LOCAL_LANE);

    private static final AttributeKey<String> ATTRIBUTE_TARGET_LANE = AttributeKey.stringKey(KEY_TARGET_LANE);

    private static final AttributeKey<Long> ATTRIBUTE_SERVICE_POLICY_ID = AttributeKey.longKey(KEY_SERVICE_POLICY_ID);

    private static final AttributeKey<String> ATTRIBUTE_SERVICE_NAME = AttributeKey.stringKey(KEY_SERVICE_NAME);

    private static final AttributeKey<String> ATTRIBUTE_SERVICE_GROUP = AttributeKey.stringKey(KEY_SERVICE_GROUP);

    private static final AttributeKey<String> ATTRIBUTE_SERVICE_PATH = AttributeKey.stringKey(KEY_SERVICE_PATH);

    private static final AttributeKey<String> ATTRIBUTE_SERVICE_METHOD = AttributeKey.stringKey(KEY_SERVICE_METHOD);

    private static final AttributeKey<String> ATTRIBUTE_REJECT_TYPE = AttributeKey.stringKey(KEY_REJECT_TYPE);

    private LongCounter gatewayInbounds;

    private LongCounter gatewayInboundForwards;

    private LongCounter gatewayInboundRejects;

    private LongCounter gatewayOutboundForwards;

    private LongCounter gatewayOutbounds;

    private LongCounter gatewayOutboundRejects;

    private LongCounter serviceInbounds;

    private LongCounter serviceInboundForwards;

    private LongCounter serviceInboundRejects;

    private LongCounter serviceOutbounds;

    private LongCounter serviceOutboundForwards;

    private LongCounter serviceOutboundRejects;

    @Config(CounterConfig.CONFIG_COUNTER)
    private CounterConfig config;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject
    private Map<String, ExporterFactory> factoryMap;

    private OpenTelemetrySdk sdk;

    @Override
    public void handle(List<Event<TrafficEvent>> events) {
        if (events != null) {
            TrafficEvent trafficEvent;
            LongCounter counter;
            for (Event<TrafficEvent> event : events) {
                trafficEvent = event.getData();
                Attributes attributes = attributes(event);
                if (config.isGatewayEnabled() && trafficEvent.getComponentType() == ComponentType.GATEWAY && trafficEvent.getDirection() == Direction.INBOUND) {
                    gatewayInbounds.add(trafficEvent.getRequests(), attributes);
                    counter = trafficEvent.getActionType() == ActionType.FORWARD ? gatewayInboundForwards : gatewayInboundRejects;
                    counter.add(trafficEvent.getRequests(), attributes);
                } else if (config.isGatewayEnabled() && trafficEvent.getComponentType() == ComponentType.GATEWAY && trafficEvent.getDirection() == Direction.OUTBOUND) {
                    gatewayOutbounds.add(trafficEvent.getRequests(), attributes);
                    counter = trafficEvent.getActionType() == ActionType.FORWARD ? gatewayOutboundForwards : gatewayOutboundRejects;
                    counter.add(trafficEvent.getRequests(), attributes);
                } else if (config.isServiceEnabled() && trafficEvent.getComponentType() == ComponentType.SERVICE && trafficEvent.getDirection() == Direction.OUTBOUND) {
                    serviceOutbounds.add(trafficEvent.getRequests(), attributes);
                    counter = trafficEvent.getActionType() == ActionType.FORWARD ? serviceOutboundForwards : serviceOutboundRejects;
                    counter.add(trafficEvent.getRequests(), attributes);
                } else if (config.isServiceEnabled() && trafficEvent.getComponentType() == ComponentType.SERVICE && trafficEvent.getDirection() == Direction.INBOUND) {
                    serviceInbounds.add(trafficEvent.getRequests(), attributes);
                    counter = trafficEvent.getActionType() == ActionType.FORWARD ? serviceInboundForwards : serviceInboundRejects;
                    counter.add(trafficEvent.getRequests(), attributes);
                }
            }
        }
    }

    private Attributes attributes(Event<TrafficEvent> event) {
        TrafficEvent trafficEvent = event.getData();
        AttributesBuilder builder = Attributes.builder();
        builder.put(ATTRIBUTE_APPLICATION, application.getName()).
                put(ATTRIBUTE_LIVE_SPACE_ID, trafficEvent.getLiveSpaceId()).
                put(ATTRIBUTE_LIVE_RULE_ID, trafficEvent.getUnitRuleId()).
                put(ATTRIBUTE_LOCAL_UNIT, trafficEvent.getLocalUnit()).
                put(ATTRIBUTE_LOCAL_CELL, trafficEvent.getLocalCell()).
                put(ATTRIBUTE_TARGET_UNIT, trafficEvent.getTargetUnit()).
                put(ATTRIBUTE_TARGET_CELL, trafficEvent.getTargetCell()).
                put(ATTRIBUTE_LIVE_DOMAIN, trafficEvent.getLiveDomain()).
                put(ATTRIBUTE_LIVE_PATH, trafficEvent.getLivePath()).
                put(ATTRIBUTE_LIVE_VARIABLE, trafficEvent.getLiveVariable()).
                put(ATTRIBUTE_LIVE_BIZ_VARIABLE, trafficEvent.getLiveBizVariable()).
                put(ATTRIBUTE_LANE_SPACE_ID, trafficEvent.getLaneSpaceId()).
                put(ATTRIBUTE_LANE_RULE_ID, trafficEvent.getLaneRuleId()).
                put(ATTRIBUTE_LOCAL_LANE, trafficEvent.getLocalLane()).
                put(ATTRIBUTE_TARGET_LANE, trafficEvent.getTargetLane()).
                put(ATTRIBUTE_SERVICE_POLICY_ID, trafficEvent.getPolicyId()).
                put(ATTRIBUTE_SERVICE_NAME, trafficEvent.getService()).
                put(ATTRIBUTE_SERVICE_GROUP, trafficEvent.getGroup()).
                put(ATTRIBUTE_SERVICE_PATH, trafficEvent.getPath()).
                put(ATTRIBUTE_SERVICE_METHOD, trafficEvent.getMethod()).
                put(ATTRIBUTE_REJECT_TYPE, trafficEvent.getRejectTypeName()).
                put(ATTRIBUTE_LOCAL_IP, event.getIp());
        if (trafficEvent.getPolicyTags() != null) {
            trafficEvent.getPolicyTags().forEach((key, value) -> builder.put(AttributeKey.stringKey(key), value));
        }
        return builder.build();
    }

    @Override
    public String getTopic() {
        return Publisher.TRAFFIC;
    }

    @Override
    public void initialize() {
        Resource resource = Resource.getDefault().toBuilder().put("service.name", application.getName()).build();
        ExporterConfig exporterConfig = config.getExporter();
        ExporterFactory factory = exporterConfig.getType() == null ? null : factoryMap.get(exporterConfig.getType());
        factory = factory == null ? new LoggingExporterFactory() : factory;
        MetricReader reader = factory.create(config);

        SdkMeterProvider provider = SdkMeterProvider.builder().setResource(resource).registerMetricReader(reader).build();
        sdk = OpenTelemetrySdk.builder().setMeterProvider(provider).buildAndRegisterGlobal();
        Meter meter = sdk.getMeter(LIVE_SCOPE);
        this.gatewayInbounds = meter.counterBuilder(COUNTER_GATEWAY_INBOUND_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.gatewayInboundForwards = meter.counterBuilder(COUNTER_GATEWAY_INBOUND_FORWARD_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.gatewayInboundRejects = meter.counterBuilder(COUNTER_GATEWAY_INBOUND_REJECT_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.gatewayOutbounds = meter.counterBuilder(COUNTER_GATEWAY_OUTBOUND_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.gatewayOutboundForwards = meter.counterBuilder(COUNTER_GATEWAY_OUTBOUND_FORWARD_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.gatewayOutboundRejects = meter.counterBuilder(COUNTER_GATEWAY_OUTBOUND_REJECT_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.serviceInbounds = meter.counterBuilder(COUNTER_SERVICE_INBOUND_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.serviceInboundForwards = meter.counterBuilder(COUNTER_SERVICE_INBOUND_FORWARD_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.serviceInboundRejects = meter.counterBuilder(COUNTER_SERVICE_INBOUND_REJECT_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.serviceOutbounds = meter.counterBuilder(COUNTER_SERVICE_OUTBOUND_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.serviceOutboundForwards = meter.counterBuilder(COUNTER_SERVICE_OUTBOUND_FORWARD_REQUESTS_TOTAL).setUnit(REQUESTS).build();
        this.serviceOutboundRejects = meter.counterBuilder(COUNTER_SERVICE_OUTBOUND_REJECT_REQUESTS_TOTAL).setUnit(REQUESTS).build();
    }

    @Override
    public void close() {
        if (sdk != null) {
            sdk.close();
        }
    }
}
