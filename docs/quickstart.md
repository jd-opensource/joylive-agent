Quick Start
===

## Start

### Quick Start

#### Preparation

> Please prepare the maven compilation environment in advance and execute the command to compile in the project root directory:
> ```bash
> mvn package -f pom.xml -DskipTests=true package
> ```

- **Compile** JoyLive Agent Release Package
- **Compile** Demo binary product compressed package
- **Download** and start Nacos

#### Get Demo binary

- Gateway

  `joylive-demo-springcloud3-gateway` Spring Cloud Gateway demo

- Application

  `joylive-demo-springcloud3-provider` Spring Cloud Application demo

#### Modify Agent configuration

The JoyLive Agent package has the following directory structure:

```bash
.
├── config
│   ├── bootstrap.properties
│   ├── config.yaml
│   ├── lanes.json
│   ├── livespaces.json
│   ├── logback.xml
│   └── microservice.json
├── lib
│   ├── core
│   │   ├── joylive-core-api-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-core-framework-1.0.0-SNAPSHOT.jar
│   │   └── joylive-governance-api-1.0.0-SNAPSHOT.jar
│   ├── core.impl
│   │   ├── joylive-bytekit-bytebuddy-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-command-lifecycle-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-event-logger-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-event-opentelemetry-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-eventbus-jbus-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-expression-jexl-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-flowcontrol-resilience4j-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-function-bkdrhash-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-logger-slf4j-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-parser-jackson-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-parser-properties-1.0.0-SNAPSHOT.jar
│   │   ├── joylive-service-file-1.0.0-SNAPSHOT.jar
│   │   └── joylive-service-watchdog-1.0.0-SNAPSHOT.jar
│   └── system
│       └── joylive-bootstrap-api-1.0.0-SNAPSHOT.jar
├── live.jar
└── plugin
    ├── dubbo
    │   ├── joylive-registry-dubbo2.6-1.0.0-SNAPSHOT.jar
    │   ├── joylive-registry-dubbo2.7-1.0.0-SNAPSHOT.jar
    │   ├── joylive-registry-dubbo3-1.0.0-SNAPSHOT.jar
    │   ├── joylive-router-dubbo2.6-1.0.0-SNAPSHOT.jar
    │   ├── joylive-router-dubbo2.7-1.0.0-SNAPSHOT.jar
    │   ├── joylive-router-dubbo3-1.0.0-SNAPSHOT.jar
    │   ├── joylive-transmission-dubbo2.6-1.0.0-SNAPSHOT.jar
    │   ├── joylive-transmission-dubbo2.7-1.0.0-SNAPSHOT.jar
    │   └── joylive-transmission-dubbo3-1.0.0-SNAPSHOT.jar
    ├── spring
    │   ├── joylive-application-springboot2-1.0.0-SNAPSHOT.jar
    │   ├── joylive-registry-springcloud3-1.0.0-SNAPSHOT.jar
    │   ├── joylive-router-springcloud3-1.0.0-SNAPSHOT.jar
    │   ├── joylive-router-springgateway3-1.0.0-SNAPSHOT.jar
    │   └── joylive-transmission-springcloud3-1.0.0-SNAPSHOT.jar
    └── system
        └── joylive-classloader-springboot2-1.0.0-SNAPSHOT.jar
```

- Modify basic application metadata

You can directly modify the `config/bootstrap.properties` file or add corresponding environment variables. The environment variable information is as follows:

| **Type** | **Name**                          | **Explanation**                                                | **Required** | **DefaultValue** | **Description**                            |
| -------- | --------------------------------- |----------------------------------------------------------------| -------- | ---------- |--------------------------------------------|
| ENV | APPLICATION_NAME                  | ApplicationName                                                | Y       |            | 建议和Spring的应用名称保持一致                         |
| ENV | APPLICATION_SERVICE_NAME          | ServiceName                                                    | N       | 应用名称   | 建议和SpringCloud的应用名称保持一致                    |
| ENV | APPLICATION_LOCATION_LIVESPACE_ID | The ID of the multi-active space where the instance is located | Y       |            |                                            |
| ENV | APPLICATION_LOCATION_UNIT         | The unit code where the instance is located                    | Y       |            |                                            |
| ENV | APPLICATION_LOCATION_CELL         | The cell code where the instance is located                    | Y       |            |                                            |
| ENV | APPLICATION_LOCATION_LANESPACE_ID | The lane space ID where the instance is located                | N       |            | Configure when enabling lane service       |
| ENV | APPLICATION_LOCATION_LANE         | The lane code where the instance is located                    | N       |            | Configure when enabling lane service       |
| ENV | APPLICATION_LOCATION_REGION       | The region where the instance is located                       | N       |            |                                            |
| ENV | APPLICATION_LOCATION_ZONE         | Availability zone where the instance is located                | N       |            |                                            |
| ENV | CONFIG_LIVE_ENABLED               | Enable multi-live flow control                                 | N       | true       | Whether to perform multi-live flow control |
| ENV | CONFIG_POLICY_INITIALIZE_TIMEOUT  | Policy sync timeout                                                         | N       | 10000(ms)  |                                            |
| ENV | CONFIG_FLOW_CONTROL_ENABLED       | Enable service flow control                                                         | N       | true       | Enable service flow control, including current limiting, circuit breaker, load balancing, label routing and other strategies               |
| ENV | CONFIG_LANE_ENABLED               | Enable lane flow control                                                         | N       | true       | Enable lane flow control                                     |
| ENV | APPLICATION_SERVICE_GATEWAY       | Gateway type                                                           | N       | NONE       | If it is set to FRONTEND for the entrance gateway, it is set to NONE for normal applications.              |

Note: When starting the `joylive-demo-springcloud3-gateway` Spring Cloud Gateway gateway demo, it needs to be set to FRONTEND. When starting `joylive-demo-springcloud3-provider` Spring Cloud application demo does not need to be set, the default is NONE.

- Modify policy synchronization

The `config` directory contains relevant files for agent configuration, as well as multi-active traffic management, microservice traffic management, and swim lane policy configuration files. The configuration location corresponds to the location in the `config/config.yaml` configuration file as follows:

| Location                    | StrategyType                           |
| ----------------------- |----------------------------------------|
| agent.sync.liveSpace    | Multi-active flow strategy             |
| agent.sync.microservice | Microservice traffic management strategy |
| agent.sync.laneSpace    | Swim lane strategy             |

In the policy configuration item, `type` corresponds to the monitoring type, and file represents the monitoring local file.

#### Start gateway

In this example, instead of modifying the configuration file, we use the method of setting environment variables.。

> Note: ${path_to_gateway_demo} is the path where joylive-demo-springcloud3-gateway demo is downloaded; ${path_to_agent} is the path where joylive-agent is downloaded and decompressed;

Start the gateway instance in simulation unit 1 with the following command:

```bash
export APPLICATION_NAME=springcloud3-gateway
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_SERVICE_GATEWAY=FRONTEND
# Set the startup nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Linux or macOS
java -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-gateway.jar -javaagent:${path_to_agent}/live.jar
# Windows
java -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-gateway.jar -javaagent:${path_to_agent}\live.jar
```

#### Start application

In this example, instead of modifying the configuration file, the method is to set environment variables.

> Note: ${path_to_provider_demo} is the path where joylive-demo-springcloud3-provider demo is downloaded; ${path_to_agent} is the path where joylive-agent is downloaded and decompressed;

Start the application instance in simulation unit 1 with the following command:

```bash
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=production
# Set the startup nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Linux or macOS
java -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}/live.jar
# Windows
java -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}\live.jar
```

Start the application instance in simulation unit 2, the command is as follows:

```bash
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit2
export APPLICATION_LOCATION_CELL=cell4
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=beta
# Set the startup nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Linux or macOS
java -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}/live.jar
# Windows
java -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}\live.jar
```

#### Effect verification

Visit the nacos registration center and check the metadata of the service instance. The following data indicates that the agent has been enhanced successfully.

```properties
unit=unit1
laneSpaceId=1
liveSpaceId=6
cell=cell1
lane=production
```

#### Traffic test

```bash
# Access the application interface through the gateway, specify the unit variable unit 1, pointing to the access unit 1 unit
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"

# Access the application interface through the gateway, specify the unit variable unit 2, point to the access unit 2 unit
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit2" -H "Host:demo.live.local"
```
