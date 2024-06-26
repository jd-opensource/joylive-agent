Quick Start
===

## 1. Quick Start

### 1.1 Preparation

> Please prepare the maven compilation environment in advance and execute the command to compile in the project root directory:
> ```bash
> mvn package -f pom.xml -DskipTests=true
> ```

- **Compile** JoyLive Agent Release Package
- **Compile** Demo binary product compressed package
- **Download** and start Nacos

### 1.2 Get Demo binary

- Gateway

  `joylive-demo-springcloud3-gateway` Spring Cloud Gateway demo

- Application

  `joylive-demo-springcloud3-provider` Spring Cloud Application demo

### 1.3 Modify Agent configuration

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
│   │   ├── joylive-core-api-1.0.0.jar
│   │   ├── joylive-core-framework-1.0.0.jar
│   │   └── joylive-governance-api-1.0.0.jar
│   ├── core.impl
│   │   ├── joylive-bytekit-bytebuddy-1.0.0.jar
│   │   ├── joylive-command-lifecycle-1.0.0.jar
│   │   ├── joylive-event-logger-1.0.0.jar
│   │   ├── joylive-event-opentelemetry-1.0.0.jar
│   │   ├── joylive-eventbus-jbus-1.0.0.jar
│   │   ├── joylive-expression-jexl-1.0.0.jar
│   │   ├── joylive-flowcontrol-resilience4j-1.0.0.jar
│   │   ├── joylive-function-bkdrhash-1.0.0.jar
│   │   ├── joylive-logger-slf4j-1.0.0.jar
│   │   ├── joylive-parser-jackson-1.0.0.jar
│   │   ├── joylive-parser-properties-1.0.0.jar
│   │   ├── joylive-service-file-1.0.0.jar
│   │   └── joylive-service-watchdog-1.0.0.jar
│   └── system
│       └── joylive-bootstrap-api-1.0.0.jar
├── live.jar
└── plugin
    ├── dubbo
    │   ├── joylive-registry-dubbo2.6-1.0.0.jar
    │   ├── joylive-registry-dubbo2.7-1.0.0.jar
    │   ├── joylive-registry-dubbo3-1.0.0.jar
    │   ├── joylive-router-dubbo2.6-1.0.0.jar
    │   ├── joylive-router-dubbo2.7-1.0.0.jar
    │   ├── joylive-router-dubbo3-1.0.0.jar
    │   ├── joylive-transmission-dubbo2.6-1.0.0.jar
    │   ├── joylive-transmission-dubbo2.7-1.0.0.jar
    │   └── joylive-transmission-dubbo3-1.0.0.jar
    ├── spring
    │   ├── joylive-application-springboot2-1.0.0.jar
    │   ├── joylive-registry-springcloud3-1.0.0.jar
    │   ├── joylive-router-springcloud3-1.0.0.jar
    │   ├── joylive-router-springgateway3-1.0.0.jar
    │   └── joylive-transmission-springcloud3-1.0.0.jar
    └── system
        └── joylive-classloader-springboot2-1.0.0.jar
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

### 1.4 Start gateway

In this example, instead of modifying the configuration file, we use the method of setting environment variables.。

> Note: ${path_to_gateway_demo} is the path where joylive-demo-springcloud3-gateway demo is downloaded; ${path_to_agent} is the path where joylive-agent is downloaded and decompressed;

Start the gateway instance in simulation unit 1 with the following command:

```bash
# Set env for Linux or macOS
export APPLICATION_NAME=springcloud3-gateway
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_SERVICE_GATEWAY=FRONTEND
# Set the startup nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-gateway.jar 

# Set env for Windows(PowerShell)
$env:APPLICATION_NAME="springcloud3-gateway"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT="unit1"
$env:APPLICATION_LOCATION_CELL="cell1"
$env:APPLICATION_SERVICE_GATEWAY="FRONTEND"
# Set the startup nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-gateway.jar
```

### 1.5 Start application

In this example, instead of modifying the configuration file, the method is to set environment variables.

> Note: ${path_to_provider_demo} is the path where joylive-demo-springcloud3-provider demo is downloaded; ${path_to_agent} is the path where joylive-agent is downloaded and decompressed;

Start the application instance in simulation unit 1 with the following command:

```bash
# Set env for Linux or macOS
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
# Set the startup nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar 

# Set env for Windows(PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT="unit1"
$env:APPLICATION_LOCATION_CELL="cell1"
# Set the startup nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

Start the application instance in simulation unit 2, the command is as follows:

```bash
# Set env for Linux or macOS
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit2
export APPLICATION_LOCATION_CELL=cell4
# Set the startup nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar 

# Set env for Windows(PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT="unit2"
$env:APPLICATION_LOCATION_CELL="cell4"
# Set the startup nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

### 1.6 Effect verification

Visit the nacos registration center and check the metadata of the service instance. The following data indicates that the agent has been enhanced successfully.

```properties
x-live-space-id=v4bEh4kd6Jvu5QBX09qYq-qlbcs
x-live-unit=unit1
x-live-cell=cell1
```

### 1.7 Traffic test

```bash
# Access the application interface through the gateway, specify the unit variable unit 1, pointing to the access unit 1 unit
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"

# Access the application interface through the gateway, specify the unit variable unit 2, point to the access unit 2 unit
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit2" -H "Host:demo.live.local"
```
## 2. Debugging

Debugging in an IDE, using IntelliJ IDEA as an example

### 2.1 Prepare Local Domain Names

Configure domain names in the `hosts` file

```
127.0.0.1 demo.live.local
127.0.0.1 unit1-demo.live.local
127.0.0.1 unit2-demo.live.local
```

### 2.2 Compile the Project

1. Compile and install the `joylive-agent` project using `mvn clean install`.
2. Obtain the full path of the compiled project output directory `target/live-${version}` from the `joylive-package`.

### 2.3 Start the Application

#### 2.3.1 Start the Registration Center

Prepare and start the `Nacos` registration center, and obtain its address, username, and password.

#### 2.3.2 Start the Gateway Project

Run the gateway application `joylive-demo-gateway`.

Configure parameters and environment variables.

![pic](./image/debug.png)

Add the following virtual machine parameter: `-javaagent:full-path-to-live-${version}`.

Refer to the following environment variable configuration:

| Name | Value | Description |
|------|-------|-------------|
| APPLICATION_LOCATION_CELL | cell4 | |
| APPLICATION_LOCATION_LIVESPACE_ID | v4bEh4kd6Jvu5QBX09qYq-qlbcs | |
| APPLICATION_LOCATION_UNIT | unit2 | |
| APPLICATION_SERVICE_GATEWAY | FRONTEND | |
| CONFIG_LOCALHOST_ENABLED | true | |
| NACOS_ADDR | | |
| NACOS_NAMESPACE | public | |
| NACOS_PASSWORD | | |
| NACOS_USERNAME | | |

For unit partition configuration, refer to the `livespaces.json` configuration file in the `joylive-package` project.

#### 2.3.3 Start the Service Project

Run the microservice application `joylive-demo-provider`, referring to the gateway application's configuration.

Refer to the following environment variable configuration:

| Name | Value | Description |
|------|-------|-------------|
| APPLICATION_LOCATION_CELL | cell1 | |
| APPLICATION_LOCATION_LIVESPACE_ID | v4bEh4kd6Jvu5QBX09qYq-qlbcs | |
| APPLICATION_LOCATION_UNIT | unit1 | |
| APPLICATION_SERVICE_GATEWAY | FRONTEND | |
| CONFIG_LOCALHOST_ENABLED | true | |
| NACOS_ADDR | | |
| NACOS_NAMESPACE | public | |
| NACOS_PASSWORD | | |
| NACOS_USERNAME | | |

### 2.4 Access Requests

Use the following curl command to send a request:

```sh
curl -G 'demo.live.local:8888/service-provider/echo/hello?user=unit1'
```