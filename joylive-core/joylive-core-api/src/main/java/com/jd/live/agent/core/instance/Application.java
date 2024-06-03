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

import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import com.jd.live.agent.core.util.tag.Label;
import lombok.Getter;
import lombok.Setter;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
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

    // Constant for the application name key.
    public static final String KEY_APPLICATION_NAME = "APPLICATION_NAME";

    // Constant for the application instance configuration key.
    public static final String CONFIG_APP_INSTANCE = "app.instance";

    // Default value used in various contexts.
    public static final String DEFAULT_VALUE = "default";

    // Constant for the application component key.
    public static final String COMPONENT_APPLICATION = "application";

    // Constants for various keys used in metadata and headers.
    public static final String KEY_INSTANCE_ID = "x-live-instance-id";

    public static final String KEY_SERVICE = "x-live-service";

    public static final String KEY_LIVE_SPACE_ID = "x-live-space-id";

    public static final String KEY_UNIT = "x-live-unit";

    public static final String KEY_CELL = "x-live-cell";

    public static final String KEY_LANE_SPACE_ID = "x-lane-space-id";

    public static final String KEY_LANE_CODE = "x-lane-code";

    // Unique application identifier.
    public static final String APP_ID = UUID.randomUUID().toString();

    // Byte representation of the application ID.
    public static final byte[] APP_ID_BYTES = APP_ID.getBytes(StandardCharsets.UTF_8);

    // Name of the application.
    @Setter
    @Config("name")
    private String name;

    // Instance identifier of the application.
    private String instance;

    // Associated service of the application.
    @Setter
    @Config("service")
    private AppService service;

    // Location information of the application.
    @Setter
    @Config("location")
    private Location location;

    // Metadata associated with the application.
    @Setter
    @Config("meta")
    private Map<String, String> meta;

    // Process ID of the application.
    private final int pid;

    @Setter
    private volatile AppStatus status = AppStatus.READY;

    /**
     * Default constructor initializes the process ID and instance with a unique application ID.
     */
    public Application() {
        this.pid = pid();
        this.instance = APP_ID;
    }

    /**
     * Custom constructor for setting application properties.
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

    public String getUniqueThreadName() {
        return "thread-" + Thread.currentThread().getId() + "@" + instance;
    }

    /**
     * Retrieves metadata value by key.
     */
    public String getMeta(String key) {
        return meta == null || key == null ? null : meta.get(key);
    }

    /**
     * Retrieves metadata value by key with a default value.
     */
    public String getMeta(String key, String defaultValue) {
        return meta == null || key == null ? defaultValue : meta.getOrDefault(key, defaultValue);
    }

    /**
     * Applies labels to the application based on its properties.
     */
    public void label(BiConsumer<String, String> consumer) {
        if (consumer != null) {
            if (location != null) {
                accept(consumer, Label.LABEL_REGION, location.getRegion());
                accept(consumer, Label.LABEL_ZONE, location.getZone());
                accept(consumer, Label.LABEL_LIVESPACE_ID, location.getLiveSpaceId());
                accept(consumer, Label.LABEL_UNIT, location.getUnit());
                accept(consumer, Label.LABEL_CELL, location.getCell());
                accept(consumer, Label.LABEL_LANESPACE_ID, location.getLaneSpaceId());
                accept(consumer, Label.LABEL_LANE, location.getLane());
                accept(consumer, Label.LABEL_CLUSTER, location.getCluster());
                accept(consumer, Label.LABEL_APPLICATION, name);
                accept(consumer, Label.LABEL_GROUP, service.getGroup());
            }
            if (service != null) {
                Map<String, String> serviceMeta = service.getMeta();
                if (serviceMeta != null) {
                    serviceMeta.forEach(consumer);
                }
            }
        }
    }

    /**
     * Helper method for applying labels.
     */
    private void accept(BiConsumer<String, String> consumer, String key, String value) {
        if (consumer != null && key != null && value != null) {
            consumer.accept(key, value);
        }
    }

    /**
     * Utility method for retrieving the process ID.
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

