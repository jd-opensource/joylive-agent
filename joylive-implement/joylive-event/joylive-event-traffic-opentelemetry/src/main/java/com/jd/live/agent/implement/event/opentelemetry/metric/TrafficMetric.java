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
package com.jd.live.agent.implement.event.opentelemetry.metric;

import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.ExporterConfig.TrafficConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

import static com.jd.live.agent.governance.event.TrafficEvent.*;

public class TrafficMetric implements Subscription<TrafficEvent> {

    private static final String KEY_COMPONENT_TYPE = "component_type";
    private static final String KEY_LIVE_SPACE_ID = "live_spaceId";
    private static final String KEY_LIVE_RULE_ID = "live_ruleId";
    private static final String KEY_LIVE_DOMAIN = "live_domain";
    private static final String KEY_LIVE_PATH = "live_path";
    private static final String KEY_LIVE_BIZ_VARIABLE = "live_bizVariable";
    private static final String KEY_LOCAL_UNIT = "local_unit";
    private static final String KEY_LOCAL_CELL = "local_cell";
    private static final String KEY_LOCAL_LANE = "local_lane";
    private static final String KEY_LOCAL_IP = "local_ip";
    private static final String KEY_TARGET_UNIT = "target_unit";
    private static final String KEY_TARGET_CELL = "target_cell";
    private static final String KEY_LANE_SPACE_ID = "lane_spaceId";
    private static final String KEY_LANE_RULE_ID = "lane_ruleId";
    private static final String KEY_TARGET_LANE = "target_lane";
    private static final String KEY_APPLICATION = "application";
    private static final String KEY_SERVICE_NAME = "service_name";
    private static final String KEY_SERVICE_GROUP = "service_group";
    private static final String KEY_SERVICE_PATH = "service_path";
    private static final String KEY_SERVICE_METHOD = "service_method";
    private static final String KEY_SERVICE_POLICY_ID = "service_policyId";
    private static final String KEY_REJECT_TYPE = "reject_type";

    private static final String COUNTER_GATEWAY_INBOUND_REQUESTS_TOTAL = "joylive_gateway_inbound_requests_total";
    private static final String COUNTER_GATEWAY_INBOUND_FORWARD_REQUESTS_TOTAL = "joylive_gateway_inbound_forward_requests_total";
    private static final String COUNTER_GATEWAY_INBOUND_REJECT_REQUESTS_TOTAL = "joylive_gateway_inbound_reject_requests_total";
    private static final String COUNTER_GATEWAY_OUTBOUND_REQUESTS_TOTAL = "joylive_gateway_outbound_requests_total";
    private static final String COUNTER_GATEWAY_OUTBOUND_FORWARD_REQUESTS_TOTAL = "joylive_gateway_outbound_forward_requests_total";
    private static final String COUNTER_GATEWAY_OUTBOUND_REJECT_REQUESTS_TOTAL = "joylive_gateway_outbound_reject_requests_total";
    private static final String COUNTER_SERVICE_INBOUND_REQUESTS_TOTAL = "joylive_service_inbound_requests_total";
    private static final String COUNTER_SERVICE_INBOUND_FORWARD_REQUESTS_TOTAL = "joylive_service_inbound_forward_requests_total";
    private static final String COUNTER_SERVICE_INBOUND_REJECT_REQUESTS_TOTAL = "joylive_service_inbound_reject_requests_total";
    private static final String COUNTER_SERVICE_OUTBOUND_REQUESTS_TOTAL = "joylive_service_outbound_requests_total";
    private static final String COUNTER_SERVICE_OUTBOUND_FORWARD_REQUESTS_TOTAL = "joylive_service_outbound_forward_requests_total";
    private static final String COUNTER_SERVICE_OUTBOUND_REJECT_REQUESTS_TOTAL = "joylive_service_outbound_reject_requests_total";

    private static final String REQUESTS = "requests";

    private static final AttributeKey<String> ATTRIBUTE_COMPONENT_TYPE = AttributeKey.stringKey(KEY_COMPONENT_TYPE);
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

    private final TrafficConfig config;
    private final Application application;

    private final LongCounter gatewayInbounds;
    private final LongCounter gatewayInboundForwards;
    private final LongCounter gatewayInboundRejects;
    private final LongCounter gatewayOutboundForwards;
    private final LongCounter gatewayOutbounds;
    private final LongCounter gatewayOutboundRejects;
    private final LongCounter serviceInbounds;
    private final LongCounter serviceInboundForwards;
    private final LongCounter serviceInboundRejects;
    private final LongCounter serviceOutbounds;
    private final LongCounter serviceOutboundForwards;
    private final LongCounter serviceOutboundRejects;

    public TrafficMetric(TrafficConfig config, Application application, Meter meter) {
        this.config = config;
        this.application = application;
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
    public void handle(List<Event<TrafficEvent>> events) {
        if (events != null) {
            TrafficEvent trafficEvent;
            LongCounter counter;
            for (Event<TrafficEvent> event : events) {
                trafficEvent = event.getData();
                Attributes attributes = attributes(event);
                if (config.isGatewayEnabled() && trafficEvent.getComponentType().isGateway() && trafficEvent.getDirection() == Direction.INBOUND) {
                    gatewayInbounds.add(trafficEvent.getRequests(), attributes);
                    counter = trafficEvent.getActionType() == ActionType.FORWARD ? gatewayInboundForwards : gatewayInboundRejects;
                    counter.add(trafficEvent.getRequests(), attributes);
                } else if (config.isGatewayEnabled() && trafficEvent.getComponentType().isGateway() && trafficEvent.getDirection() == Direction.OUTBOUND) {
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

    @Override
    public String getTopic() {
        return Publisher.TRAFFIC;
    }

    private Attributes attributes(Event<TrafficEvent> event) {
        TrafficEvent trafficEvent = event.getData();
        AttributesBuilder builder = Attributes.builder();
        builder.put(ATTRIBUTE_COMPONENT_TYPE, trafficEvent.getComponentType().name()).
                put(ATTRIBUTE_APPLICATION, application.getName()).
                put(ATTRIBUTE_LIVE_SPACE_ID, trafficEvent.getLiveSpaceId()).
                put(ATTRIBUTE_LIVE_RULE_ID, trafficEvent.getUnitRuleId()).
                put(ATTRIBUTE_LOCAL_UNIT, trafficEvent.getLocalUnit()).
                put(ATTRIBUTE_LOCAL_CELL, trafficEvent.getLocalCell()).
                put(ATTRIBUTE_TARGET_UNIT, trafficEvent.getTargetUnit()).
                put(ATTRIBUTE_TARGET_CELL, trafficEvent.getTargetCell()).
                put(ATTRIBUTE_LIVE_DOMAIN, trafficEvent.getLiveDomain()).
                put(ATTRIBUTE_LIVE_PATH, trafficEvent.getLivePath()).
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
}
