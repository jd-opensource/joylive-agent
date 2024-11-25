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
package com.jd.live.agent.core.instance;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Represents the application configuration and metadata within a system.
 * This class encapsulates various attributes related to the application,
 * such as its name, instance information, associated service, and location.
 * It also provides methods for accessing and manipulating application metadata.
 * Additionally, it includes constants for key metadata attributes and utility methods
 * for labeling and identifying the application process.
 *
 * @since 1.0.0
 */
@Getter
@Configurable(prefix = "app")
public class Application {

    /**
     * Constant for the application name key.
     */
    public static final String KEY_APPLICATION_NAME = "APPLICATION_NAME";

    /**
     * Constant for the application instance configuration key.
     */
    public static final String CONFIG_APP_INSTANCE = "app.instance";

    /**
     * Constant for the application component key.
     */
    public static final String COMPONENT_APPLICATION = "application";

    /**
     * Unique application identifier.
     */
    public static final String APP_ID = UUID.randomUUID().toString();

    /**
     * Name of the application.
     */
    @Setter
    @Config("name")
    private String name;

    /**
     * Instance identifier of the application.
     */
    private String instance;

    /**
     * Associated service of the application.
     */
    @Setter
    @Config("service")
    private AppService service;

    /**
     * Location information of the application.
     */
    @Setter
    @Config("location")
    private Location location;

    /**
     * Metadata associated with the application.
     */
    @Setter
    @Config("meta")
    private Map<String, String> meta;

    /**
     * Process ID of the application.
     */
    private final int pid;

    @Setter
    private volatile AppStatus status = AppStatus.STARTING;

    @Setter
    private Long timestamp;

    /**
     * Default constructor initializes the process ID and instance with a unique application ID.
     */
    public Application() {
        this.pid = pid();
        this.instance = APP_ID;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Custom constructor for setting application properties.
     *
     * @param name     application name
     * @param instance application instance
     * @param service  associated service
     * @param location location information
     * @param meta     metadata
     */
    public Application(String name, String instance, AppService service, Location location, Map<String, String> meta) {
        this.name = name;
        this.instance = instance;
        this.service = service;
        this.location = location;
        this.meta = meta;
        this.pid = pid();
        this.instance = APP_ID;
    }

    /**
     * Retrieves metadata value by key.
     *
     * @param key metadata key
     * @return metadata value
     */
    public String getMeta(String key) {
        return meta == null || key == null ? null : meta.get(key);
    }

    /**
     * Retrieves metadata value by key with a default value.
     *
     * @param key          metadata key
     * @param defaultValue default value
     * @return metadata value
     */
    public String getMeta(String key, String defaultValue) {
        return meta == null || key == null ? defaultValue : meta.getOrDefault(key, defaultValue);
    }

    /**
     * Labels the given metadata with a set of predefined keys and values.
     *
     * @param metadata A Map of metadata to be labeled.
     * @param consumer A BiConsumer that will be called for each key-value pair.
     */
    public static void label(Map<String, String> metadata, BiConsumer<String, String> consumer) {
        accept(consumer, Constants.LABEL_APPLICATION, metadata.get(Constants.LABEL_APPLICATION));
        accept(consumer, Constants.LABEL_INSTANCE_ID, metadata.get(Constants.LABEL_INSTANCE_ID));
        accept(consumer, Constants.LABEL_TIMESTAMP, metadata.get(Constants.LABEL_TIMESTAMP));
        accept(consumer, Constants.LABEL_CLOUD, metadata.get(Constants.LABEL_CLOUD));
        accept(consumer, Constants.LABEL_REGION, metadata.get(Constants.LABEL_REGION));
        accept(consumer, Constants.LABEL_ZONE, metadata.get(Constants.LABEL_ZONE));
        accept(consumer, Constants.LABEL_CLUSTER, metadata.get(Constants.LABEL_CLUSTER));
        accept(consumer, Constants.LABEL_LIVE_SPACE_ID, metadata.get(Constants.LABEL_LIVE_SPACE_ID));
        accept(consumer, Constants.LABEL_RULE_ID, metadata.get(Constants.LABEL_RULE_ID));
        accept(consumer, Constants.LABEL_UNIT, metadata.get(Constants.LABEL_UNIT));
        accept(consumer, Constants.LABEL_CELL, metadata.get(Constants.LABEL_CELL));
        accept(consumer, Constants.LABEL_LANE_SPACE_ID, metadata.get(Constants.LABEL_LANE_SPACE_ID));
        accept(consumer, Constants.LABEL_LANE, metadata.get(Constants.LABEL_LANE));
        accept(consumer, Constants.LABEL_WEIGHT, metadata.get(Constants.LABEL_WEIGHT));
        accept(consumer, Constants.LABEL_WARMUP, metadata.get(Constants.LABEL_WARMUP));
        accept(consumer, Constants.LABEL_SERVICE_GROUP, metadata.get(Constants.LABEL_SERVICE_GROUP));
    }

    /**
     * Applies labels to the application based on its properties.
     *
     * @param consumer label consumer
     */
    public void labelRegistry(BiConsumer<String, String> consumer) {
        labelRegistry(consumer, false);
    }

    /**
     * Applies labels to the application based on its properties.
     *
     * @param consumer     label consumer
     * @param serviceGroup a flag indicating whether to include the service group in the labeling process.
     */
    public void labelRegistry(BiConsumer<String, String> consumer, boolean serviceGroup) {
        if (consumer != null) {
            labelInstance(consumer);
            if (location != null) {
                labelZone(consumer);
                labelLiveSpace(consumer);
                labelLane(consumer);
            }
            labelService(consumer, serviceGroup);
        }
    }

    /**
     * Applies sync metadata to the application based on its properties.
     *
     * @param consumer label consumer
     */
    public void labelSync(BiConsumer<String, String> consumer) {
        if (consumer != null) {
            labelInstance(consumer);
            if (location != null) {
                labelZone(consumer);
                labelLiveSpace(consumer);
                labelLane(consumer);
                accept(consumer, Constants.LABEL_INSTANCE_IP, location.getIp());
            }
            if (meta != null) {
                accept(consumer, Constants.LABEL_AGENT_VERSION, meta.get(Constants.LABEL_AGENT_VERSION));
            }
        }
    }

    /**
     * Labels the instance information using the provided consumer.
     *
     * @param consumer the consumer to use for labeling
     */
    private void labelInstance(BiConsumer<String, String> consumer) {
        accept(consumer, Constants.LABEL_APPLICATION, name);
        accept(consumer, Constants.LABEL_INSTANCE_ID, instance);
        accept(consumer, Constants.LABEL_TIMESTAMP, timestamp == null ? null : String.valueOf(timestamp));
    }

    /**
     * Labels the service information using the provided consumer.
     *
     * @param consumer the BiConsumer to use for labeling. It takes two parameters: the label key and the label value.
     * @param group    a flag indicating whether to include the service group in the labeling process.
     */
    private void labelService(BiConsumer<String, String> consumer, boolean group) {
        if (service != null) {
            accept(consumer, Constants.LABEL_WEIGHT, service.getWeight() == null ? null : service.getWeight().toString());
            accept(consumer, Constants.LABEL_WARMUP, service.getWarmupDuration() == null ? null : service.getWarmupDuration().toString());
            accept(consumer, Constants.LABEL_SERVICE_GROUP, !group ? null : service.getGroup());
            Map<String, String> serviceMeta = service.getMeta();
            if (serviceMeta != null) {
                serviceMeta.forEach(consumer);
            }
        }
    }

    /**
     * Labels the zone information using the provided consumer.
     *
     * @param consumer the consumer to use for labeling
     */
    private void labelZone(BiConsumer<String, String> consumer) {
        accept(consumer, Constants.LABEL_CLOUD, location.getCloud());
        accept(consumer, Constants.LABEL_REGION, location.getRegion());
        accept(consumer, Constants.LABEL_ZONE, location.getZone());
        accept(consumer, Constants.LABEL_CLUSTER, location.getCluster());
    }

    /**
     * Labels the live space information using the provided consumer.
     *
     * @param consumer the consumer to use for labeling
     */
    private void labelLiveSpace(BiConsumer<String, String> consumer) {
        if (!location.isLiveless()) {
            accept(consumer, Constants.LABEL_LIVE_SPACE_ID, location.getLiveSpaceId());
            accept(consumer, Constants.LABEL_RULE_ID, location.getUnitRuleId());
            accept(consumer, Constants.LABEL_UNIT, location.getUnit());
            accept(consumer, Constants.LABEL_CELL, location.getCell());
        }
    }

    /**
     * Labels the lane information using the provided consumer.
     *
     * @param consumer the consumer to use for labeling
     */
    private void labelLane(BiConsumer<String, String> consumer) {
        if (!location.isLaneless()) {
            accept(consumer, Constants.LABEL_LANE_SPACE_ID, location.getLaneSpaceId());
            accept(consumer, Constants.LABEL_LANE, location.getLane());
        }
    }

    /**
     * Helper method for applying labels.
     *
     * @param consumer label consumer
     * @param key      label key
     * @param value    label value
     */
    private static void accept(BiConsumer<String, String> consumer, String key, String value) {
        if (consumer != null && key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            consumer.accept(key, value);
        }
    }

    /**
     * Utility method for retrieving the process ID.
     *
     * @return process ID
     */
    private static int pid() {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        try {
            return Integer.parseInt(processName.split("@")[0]);
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }

}

