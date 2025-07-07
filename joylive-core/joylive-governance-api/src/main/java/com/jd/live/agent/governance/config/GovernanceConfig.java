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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.instance.Application;
import lombok.Getter;
import lombok.Setter;

/**
 * The {@code GovernanceConfig} class is a configuration holder for various governance features within an application.
 * It includes configurations for live service management, lane management, transmission strategies, registry settings,
 * classloader preferences, flow control, protection mechanisms against specific technologies, and counter configurations.
 * <p>
 * This class uses Lombok annotations {@code @Getter} and {@code @Setter} to auto-generate getter and setter methods
 * for its fields, reducing boilerplate code.
 * <p>
 * Configuration constants defined within this class are used as keys for accessing and modifying corresponding
 * configuration values. These include enabling/disabling features and specifying behavior for different aspects
 * of the application's governance.
 *
 * @see LiveConfig
 * @see ServiceConfig
 * @see LaneConfig
 */
@Getter
@Setter
public class GovernanceConfig {

    public static final String COMPONENT_GOVERNANCE_CONFIG = "governanceConfig";
    public static final String CONFIG_AGENT_GOVERNANCE = "agent.governance";

    protected static final String ENABLED = ".enabled";
    protected static final String CONFIG_SWITCH = "agent.switch";
    protected static final String CONFIG_SWITCH_LIVE = CONFIG_SWITCH + ".live";
    protected static final String CONFIG_SWITCH_GOVERN = CONFIG_SWITCH + ".govern";
    protected static final String CONFIG_SWITCH_LANE = CONFIG_SWITCH + ".lane";
    protected static final String CONFIG_SWITCH_CONFIG_CENTER = CONFIG_SWITCH + ".configCenter";
    protected static final String CONFIG_SWITCH_FLOW_CONTROL = CONFIG_SWITCH + ".flowcontrol";
    protected static final String CONFIG_SWITCH_PROTECT = CONFIG_SWITCH + ".protect";
    protected static final String CONFIG_SWITCH_FAILOVER = CONFIG_SWITCH + ".failover";
    protected static final String CONFIG_SWITCH_TRANSMISSION = CONFIG_SWITCH + ".transmission";
    protected static final String CONFIG_SWITCH_REGISTRY = CONFIG_SWITCH + ".registry";
    protected static final String CONFIG_SWITCH_CLASSLOADER = CONFIG_SWITCH + ".classloader";
    protected static final String CONFIG_SWITCH_COUNTER = CONFIG_SWITCH + ".counter";

    public static final String CONFIG_CLASSLOADER_ENABLED = CONFIG_SWITCH_CLASSLOADER + ENABLED;
    public static final String CONFIG_CLASSLOADER_SPRING_BOOT_ENABLED = CONFIG_SWITCH_CLASSLOADER + ".springboot";

    public static final String CONFIG_LIVE_ENABLED = CONFIG_SWITCH_LIVE + ENABLED;

    public static final String CONFIG_GOVERN_ENABLED = CONFIG_SWITCH_GOVERN + ENABLED;
    public static final String CONFIG_GOVERN_DUBBO_ENABLED = CONFIG_SWITCH_GOVERN + ".dubbo";
    public static final String CONFIG_GOVERN_GRPC_ENABLED = CONFIG_SWITCH_GOVERN + ".grpc";
    public static final String CONFIG_GOVERN_SOFARPC_ENABLED = CONFIG_SWITCH_GOVERN + ".sofarpc";
    public static final String CONFIG_GOVERN_JSF_ENABLED = CONFIG_SWITCH_GOVERN + ".jsf";
    public static final String CONFIG_GOVERN_SPRING_ENABLED = CONFIG_SWITCH_GOVERN + ".spring";
    public static final String CONFIG_GOVERN_SPRING_GATEWAY_ENABLED = CONFIG_SWITCH_GOVERN + ".springgateway";
    public static final String CONFIG_GOVERN_ZUUL_ENABLED = CONFIG_SWITCH_GOVERN + ".zuul";
    public static final String CONFIG_GOVERN_PHEVOS_ENABLED = CONFIG_SWITCH_GOVERN + ".phevos";
    public static final String CONFIG_GOVERN_MQ_ENABLED = CONFIG_SWITCH_GOVERN + ".mq";
    public static final String CONFIG_GOVERN_ROCKETMQ_ENABLED = CONFIG_SWITCH_GOVERN + ".rocketmq";
    public static final String CONFIG_GOVERN_KAFKA_ENABLED = CONFIG_SWITCH_GOVERN + ".kafka";
    public static final String CONFIG_GOVERN_PULSAR_ENABLED = CONFIG_SWITCH_GOVERN + ".pulsar";

    public static final String CONFIG_LANE_ENABLED = CONFIG_SWITCH_LANE + ENABLED;

    public static final String CONFIG_TRANSMISSION_ENABLED = CONFIG_SWITCH_TRANSMISSION + ENABLED;
    public static final String CONFIG_TRANSMISSION_THREADPOOL_ENABLED = CONFIG_SWITCH_TRANSMISSION + ".threadpool";

    public static final String CONFIG_FLOW_CONTROL_ENABLED = CONFIG_SWITCH_FLOW_CONTROL + ENABLED;
    public static final String CONFIG_LOCALHOST_ENABLED = CONFIG_SWITCH_FLOW_CONTROL + ".localhost";
    public static final String CONFIG_VIRTUAL_THREAD_ENABLED = CONFIG_SWITCH_FLOW_CONTROL + ".virtualthread";
    public static final String CONFIG_CIRCUIT_BREAK_LOG_ENABLED = CONFIG_SWITCH_FLOW_CONTROL + ".circuitbreaklog";

    public static final String CONFIG_PROTECT_ENABLED = CONFIG_SWITCH_PROTECT + ENABLED;
    public static final String CONFIG_PROTECT_DB_ENABLED = CONFIG_SWITCH_PROTECT + ".db";
    public static final String CONFIG_PROTECT_MQ_ENABLED = CONFIG_SWITCH_PROTECT + ".mq";
    public static final String CONFIG_PROTECT_MARIADB_ENABLED = CONFIG_SWITCH_PROTECT + ".mariadb";
    public static final String CONFIG_PROTECT_POSTGRESQL_ENABLED = CONFIG_SWITCH_PROTECT + ".postgresql";
    public static final String CONFIG_PROTECT_OPENGAUSS_ENABLED = CONFIG_SWITCH_PROTECT + ".opengauss";
    public static final String CONFIG_PROTECT_MYSQL_ENABLED = CONFIG_SWITCH_PROTECT + ".mysql";
    public static final String CONFIG_PROTECT_DMDB_ENABLED = CONFIG_SWITCH_PROTECT + ".dmdb";
    public static final String CONFIG_PROTECT_MONGODB_ENABLED = CONFIG_SWITCH_PROTECT + ".mongodb";
    public static final String CONFIG_PROTECT_ROCKETMQ_ENABLED = CONFIG_SWITCH_PROTECT + ".rocketmq";
    public static final String CONFIG_PROTECT_REDIS_ENABLED = CONFIG_SWITCH_PROTECT + ".redis";

    public static final String CONFIG_FAILOVER_ENABLED = CONFIG_SWITCH_FAILOVER + ENABLED;
    public static final String CONFIG_FAILOVER_DB_ENABLED = CONFIG_SWITCH_FAILOVER + ".db";
    public static final String CONFIG_FAILOVER_MQ_ENABLED = CONFIG_SWITCH_FAILOVER + ".mq";
    public static final String CONFIG_FAILOVER_MONGODB_ENABLED = CONFIG_SWITCH_FAILOVER + ".mongodb";
    public static final String CONFIG_FAILOVER_ROCKETMQ_ENABLED = CONFIG_SWITCH_FAILOVER + ".rocketmq";
    public static final String CONFIG_FAILOVER_REDIS_ENABLED = CONFIG_SWITCH_FAILOVER + ".redis";

    public static final String CONFIG_COUNTER_ENABLED = CONFIG_SWITCH_COUNTER + ENABLED;

    public static final String CONFIG_ROUTER = CONFIG_AGENT_GOVERNANCE + ".router";
    public static final String CONFIG_ROUTER_SPRING = CONFIG_ROUTER + ".spring";
    public static final String CONFIG_ROUTER_SPRING_DISCOVERY_DISABLES = CONFIG_ROUTER_SPRING + ".discovery.disables";

    public static final String CONFIG_REGISTRY = CONFIG_AGENT_GOVERNANCE + ".registry";
    public static final String CONFIG_REGISTRY_ENABLED = CONFIG_REGISTRY + ENABLED;

    public static final String CONFIG_CENTER = CONFIG_AGENT_GOVERNANCE + ".configCenter";
    public static final String CONFIG_CENTER_ENABLED = CONFIG_CENTER + ENABLED;

    public static final String CONFIG_PROBE = CONFIG_AGENT_GOVERNANCE + ".probe";
    public static final String CONFIG_PROBE_ZOOKEEPER = CONFIG_PROBE + ".zookeeper";
    public static final String CONFIG_PROBE_NACOS = CONFIG_PROBE + ".nacos";

    @Config("live")
    private LiveConfig liveConfig = new LiveConfig();

    @Config("lane")
    private LaneConfig laneConfig = new LaneConfig();

    @Config("service")
    private ServiceConfig serviceConfig = new ServiceConfig();

    @Config("mq")
    private MqConfig mqConfig = new MqConfig();

    @Config("registry")
    private RegistryConfig registryConfig = new RegistryConfig();

    @Config("transmission")
    private TransmitConfig transmitConfig = new TransmitConfig();

    @Config("configCenter")
    private ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();

    @Config("db")
    private DbConfig dbConfig = new DbConfig();

    @Config("redis")
    private RedisConfig redisConfig = new RedisConfig();

    @Config
    private int initializeTimeout = 10 * 1000;

    public GovernanceConfig() {
    }

    public GovernanceConfig(LiveConfig liveConfig) {
        this.liveConfig = liveConfig;
    }

    public void initialize(Application application) {
        MonitorConfig monitorConfig = serviceConfig.getMonitor();
        if (monitorConfig == null) {
            monitorConfig = new MonitorConfig(application.getService().isFrontGateway(), true);
            serviceConfig.setMonitor(monitorConfig);
        } else if (monitorConfig.getForwardEnabled() == null) {
            monitorConfig.setForwardEnabled(application.getService().isFrontGateway());
        }
        serviceConfig.initialize();
        redisConfig.initialize();
    }
}
