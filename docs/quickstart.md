Quick Start
===

## 1. Quick Start

### 1.1 Getting the Agent Program

#### 1.1.1 Downloading the Binary Package

- Download the latest binary package (zip or tar.gz) from [Release](https://github.com/jd-opensource/joylive-agent/releases)
- Download the latest binary package (zip or tar.gz) from [Maven-1](https://mvnrepository.com/artifact/com.jd.live/joylive-package) or [Maven-2](https://search.maven.org/search?q=g:com.jd.live%20AND%20a:joylive-package) repository

#### 1.1.2 Compiling Manually

> Prepare the Maven compilation environment in advance. Execute the command in the project root directory to compile:
> ```bash
> mvn package -f pom.xml -DskipTests=true
> ```

- **Compilation Obtained** JoyLive Agent Release package, path: `joylive-package/target/live-x.x.x-SNAPSHOT`

### 1.2 Getting the Demo Program

Compile [joylive-demo](https://github.com/jd-opensource/joylive-demo) to obtain the binary packages of each project under `joylive-demo/joylive-demo-springcloud2021`

- Gateway

  `joylive-demo-springcloud2021-gateway` Spring Cloud Gateway demo

- Application

  `joylive-demo-springcloud2021-provider` Spring Cloud application demo

### 1.3 Modifying Configurations

The JoyLive Agent package has the following directory structure:

```bash
.
├── config
│   ├── bootstrap.properties
│   ├── config.yaml
│   ├── lanes.json
│   ├── livespaces.json
│   ├── logback.xml
│   └── microservice.json
├── lib
│   ├── core
│   │   ├── joylive-core-api-1.0.0.jar
│   │   ├── joylive-core-framework-1.0.0.jar
│   │   └── joylive-governance-api-1.0.0.jar
│   ├── core.impl
│   │   ├── joylive-bytekit-bytebuddy-1.0.0.jar
│   │   ├── joylive-command-lifecycle-1.0.0.jar
│   │   ├── joylive-event-logger-1.0.0.jar
│   │   ├── joylive-event-opentelemetry-1.0.0.jar
│   │   ├    ...
├── live.jar
└── plugin
    ├── dubbo
    │   ├── joylive-registry-dubbo2.6-1.0.0.jar
    │   ├── joylive-registry-dubbo2.7-1.0.0.jar
    │   ├── joylive-registry-dubbo3-1.0.0.jar
    │   ├── joylive-router-dubbo2.6-1.0.0.jar
    │   ├── joylive-router-dubbo2.7-1.0.0.jar
    │   ├    ...
    ├── spring
    │   ├── joylive-application-springboot2-1.0.0.jar
    │   ├── joylive-registry-springcloud3-1.0.0.jar
    │   ├    ...
    └    ...
```

- Modifying Configuration Files

Files in the `config` directory are related to agent configurations and multi-active traffic governance, microservice traffic governance, and lane strategy configuration files.

Default policies are loaded from local files and can be configured to load remotely.

| Location                    | Strategy Type      |
| --------------------------- | ---------------- |
| agent.sync.liveSpace        | Multi-active Traffic Strategy    |
| agent.sync.microservice     | Microservice Traffic Governance Strategy |
| agent.sync.laneSpace        | Lane Strategy      |

In the policy configuration items, `type` corresponds to the listening type, with file representing local file listening.

- Modifying Environment Variables

Common environment variables are as follows, and more can be found in the [Configuration Reference Manual](./config.md).

| **Name**                          | **Description**           | **Required** | **Default Value**   | **Description**                                                 |
| --------------------------------- | ------------------------ | ------------ |------------------- | ---------------------------------------------------------------- |
| APPLICATION_NAME                  | Application Name         | Yes          |                     | Recommended to keep consistent with the Spring application name       |
| APPLICATION_SERVICE_NAME          | Service Name             | No           | Application Name    | Recommended to keep consistent with the SpringCloud application name |
| APPLICATION_LOCATION_LIVESPACE_ID | Instance's Multi-active Space ID | Yes          |                     |                                                                 |
| APPLICATION_LOCATION_UNIT         | Instance's Unit Code     | Yes          |                     |                                                                 |
| APPLICATION_LOCATION_CELL         | Instance's Partition Code | Yes          |                     |                                                                 |
| APPLICATION_LOCATION_LANESPACE_ID | Instance's Lane Space ID | No           |                     | Configured when enabling lane service                             |
| APPLICATION_LOCATION_LANE         | Instance's Lane Code     | No           |                     | Configured when enabling lane service                             |
| APPLICATION_LOCATION_REGION       | Instance's Region        | No           |                     |                                                                 |
| APPLICATION_LOCATION_ZONE         | Instance's Availability Zone | No           |                     |                                                                 |
| CONFIG_LIVE_ENABLED               | Enable Multi-active Flow Control | No           | false             | Whether to perform multi-active flow control                     |
| CONFIG_POLICY_INITIALIZE_TIMEOUT  | Policy Synchronization Timeout | No           | 10000(ms)          |                                                                 |
| CONFIG_FLOW_CONTROL_ENABLED       | Enable Service Flow Control | No           | false             | Enable service flow control, including rate limiting, circuit breaking, load balancing, label routing, and other strategies |
| CONFIG_LANE_ENABLED               | Enable Lane Flow Control  | No           | false             | Enable lane flow control                                         |
| APPLICATION_SERVICE_GATEWAY       | Gateway Type             | No           | NONE              | Set to FRONTEND for entry gateway and NONE for regular applications  |

Note: When starting `joylive-demo-springcloud3-gateway` Spring Cloud Gateway demo, set it to FRONTEND. Starting `joylive-demo-springcloud3-provider` Spring Cloud application demo does not require setting, default is NONE.

### 1.4 Unit Multi-active

#### 1.4.1 Starting the Gateway

In this example, the method of setting environment variables instead of modifying configuration files is adopted.

> Note:
> - ${path_to_gateway_demo} is the path where `joylive-demo-springcloud3-gateway.jar` is located;
> - ${path_to_agent} is the path where `joylive.jar` is located.
> - The local policy file for multi-active is livespaces.json

To simulate starting a gateway instance within unit 1, the command is as follows:

```bash
# On Linux or macOS, set environment variables
export APPLICATION_NAME=springcloud3-gateway
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT_RULE_ID=1003
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_SERVICE_GATEWAY=FRONTEND
export CONFIG_LIVE_ENABLED=true
# Set the startup Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-gateway.jar

# On Windows, set environment variables (PowerShell)
$env:APPLICATION_NAME="springcloud3-gateway"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT_RULE_ID="1003"
$env:APPLICATION_LOCATION_UNIT="unit1"
$env:APPLICATION_LOCATION_CELL="cell1"
$env:APPLICATION_SERVICE_GATEWAY="FRONTEND"
$env:CONFIG_LIVE_ENABLED="true"
# Set the startup Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-gateway.jar
```

#### 1.4.2 Starting the Application

Follow the gateway startup method and simulate starting an application instance within unit 1, the command is as follows:

```bash
# On Linux or macOS, set environment variables
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT_RULE_ID=1003
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export CONFIG_LIVE_ENABLED=true
# Set the startup Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar

# On Windows, set environment variables (PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT_RULE_ID="1003"
$env:APPLICATION_LOCATION_UNIT="unit1"
$env:APPLICATION_LOCATION_CELL="cell1"
$env:CONFIG_LIVE_ENABLED="true"
# Set the startup Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

To simulate starting an application instance within unit 2, the command is as follows:

```bash
# On Linux or macOS, set environment variables
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT_RULE_ID=1003
export APPLICATION_LOCATION_UNIT=unit2
export APPLICATION_LOCATION_CELL=cell4
export APPLICATION_NAME=springcloud3-provider
export CONFIG_LIVE_ENABLED=true
# Set the startup Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar

# On Windows, set environment variables (PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT_RULE_ID="1003"
$env:APPLICATION_LOCATION_UNIT="unit2"
$env:APPLICATION_LOCATION_CELL="cell4"
$env:CONFIG_LIVE_ENABLED="true"
# Set the startup Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

#### 1.4.3 Verifying Registration

Access the `nacos` registration center to check if the service instance metadata contains the following data, which represents that the agent enhancement is successful.

```properties
x-live-space-id=v4bEh4kd6Jvu5QBX09qYq-qlbcs
x-live-unit=unit1
x-live-cell=cell1
```

#### 1.4.4 Traffic Testing

Refer to the rules in the multi-active strategy file configuration:

```bash
# Access the application interface through the gateway, specifying the unit variable unit1, pointing to the access of unit1.
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"

# Access the application interface through the gateway, specifying the unit variable unit2, pointing to the access of unit2.
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit2" -H "Host:demo.live.local"
```

### 1.5 Lanes and Service Governance

#### 1.5.1 Starting the Gateway

In this example, the method of setting environment variables instead of modifying configuration files is adopted.

> Note:
> - ${path_to_gateway_demo} is the path where `joylive-demo-springcloud3-gateway.jar` is located;
> - ${path_to_agent} is the path where `joylive.jar` is located.
> - The local policy file for lanes is lanes.json
> - The local policy file for microservices is microservice.json

To simulate starting a gateway instance within the production lane, the command is as follows:

```bash
# On Linux or macOS, set environment variables
export APPLICATION_NAME=springcloud3-gateway
export APPLICATION_SERVICE_GATEWAY=FRONTEND
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=production
export CONFIG_FLOW_CONTROL_ENABLED=true
export CONFIG_LANE_ENABLED=true
# Set the startup Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-gateway.jar

# On Windows, set environment variables (PowerShell)
$env:APPLICATION_NAME="springcloud3-gateway"
$env:APPLICATION_SERVICE_GATEWAY="FRONTEND"
$env:APPLICATION_LOCATION_LANESPACE_ID="1"
$env:APPLICATION_LOCATION_LANE="production"
$env:CONFIG_FLOW_CONTROL_ENABLED="true"
$env:CONFIG_LANE_ENABLED="true"
# Set the startup Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-gateway.jar
```

#### 1.5.2 Starting the Application

Follow the gateway startup method and simulate starting an application instance within the production lane, the command is as follows:

```bash
# On Linux or macOS, set environment variables
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=production
export CONFIG_FLOW_CONTROL_ENABLED=true
export CONFIG_LANE_ENABLED=true
# Set the startup Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar

# On Windows, set environment variables (PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LANESPACE_ID="1"
$env:APPLICATION_LOCATION_LANE="production"
$env:CONFIG_FLOW_CONTROL_ENABLED="true"
$env:CONFIG_LANE_ENABLED="true"
# Set the startup Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

To simulate starting an application instance within the beta lane, the command is as follows:

```bash
# On Linux or macOS, set environment variables
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=beta
export CONFIG_FLOW_CONTROL_ENABLED=true
export CONFIG_LANE_ENABLED=true
# Set the startup Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar

# On Windows, set environment variables (PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LANESPACE_ID="1"
$env:APPLICATION_LOCATION_LANE="beta"
$env:CONFIG_FLOW_CONTROL_ENABLED="true"
$env:CONFIG_LANE_ENABLED="true"
# Set the startup Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

#### 1.5.3 Verifying Registration

Access the `nacos` registration center to check if the service instance metadata contains the following data, which represents that the agent enhancement is successful.

```properties
x-lane-space-id=1
x-lane-code=production
```

#### 1.5.4 Traffic Testing

Refer to the tinting rules in the lane strategy file configuration:

```bash
# Access the application interface through the gateway, routing to the production lane
curl -X GET "http://localhost:8888/service-provider/echo/abc?aaa=false" -H "Host:demo.live.local"

# Access the application interface through the gateway, routing to the beta lane
curl -X GET "http://localhost:8888/service-provider/echo/abc?aaa=true" -H "Host:demo.live.local"
```

## 2. Debugging

Debugging in an IDE, with IntelliJ Idea as an example.

### 2.1 Compiling the Project

1. Compile and install the `joylive-agent` project with `mvn clean install`.
2. Obtain the full path to the `target/live-${version}` directory of the compiled `joylive-package` project.

### 2.2 Starting the Application

#### 2.2.1 Starting the Registration Center

Prepare and start the `Nacos` registration center to obtain its address, username, and password.

#### 2.2.2 Starting the Gateway Project

Run the gateway application `joylive-demo-gateway`.

Configure parameters and environment variables.

![pic](image/debug.png)

Add the full path of the `live-${version}` to the Java agent parameter.

Refer to the following for configuration environment variables.

| Name | Value | Description |
|------|-------|-------------|
| APPLICATION_LOCATION_CELL | cell4 |          |
| APPLICATION_LOCATION_LIVESPACE_ID | v4bEh4kd6Jvu5QBX09qYq-qlbcs |          |
| APPLICATION_LOCATION_UNIT | unit2 |          |
| APPLICATION_SERVICE_GATEWAY | FRONTEND |          |
| CONFIG_LOCALHOST_ENABLED | true |          |
| NACOS_ADDR |        |          |
| NACOS_NAMESPACE | public |          |
| NACOS_PASSWORD |        |          |
| NACOS_USERNAME |        |          |

The configuration of the unit partition should refer to the configuration file `livespaces.json` in the `joylive-package` project.

#### 2.2.3 Starting the Service Project

Run the microservice application `joylive-demo-provider`, referring to the configuration of the gateway application.

Refer to the following for configuration environment variables.

| Name | Value                           | Description |
|------|-------------------------------|-------------|
| APPLICATION_LOCATION_CELL | cell1 |          |
| APPLICATION_LOCATION_LIVESPACE_ID | v4bEh4kd6Jvu5QBX09qYq-qlbcs |          |
| APPLICATION_LOCATION_UNIT | unit1 |          |
| CONFIG_LOCALHOST_ENABLED | true |          |
| NACOS_ADDR |                           |          |
| NACOS_NAMESPACE | public |          |
| NACOS_PASSWORD |                           |          |
| NACOS_USERNAME |                           |          |

### 2.3 Making a Request

Use the following command to make a request.

```shell
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"
```

## 3. Supplements

### 3.1 Rocketmq

1. Prepare the Rocketmq environment and start the related services.
2. Referring to the application configuration above, prepare the running environment for `joylive-demo-rocketmq`. Additional environment variables need to be configured as follows.

| Variable | Noun | Description |
|----------|------|-------------|
| CONFIG_LIVE_MQ_ENABLED | Enable MQ Identifier | Whether to enable MQ <li>true: enable</li><li>not enabled</li> |
| CONFIG_LIVE_TOPICS | Multi-active Topic Names | Multiple topics are separated by commas |
| ROCKETMQ_ADDR | Rocketmq Service Address | IP:Port |

3. Refer to the above configuration to start the `joylive-demo-springcloud3-gateway` for Unit 1.
4. Refer to the above configuration to start the `joylive-demo-springcloud3-provider` for Unit 1.
5. Refer to the above configuration to start the `joylive-demo-rocketmq` for Unit 1.
6. Access verification

```shell
curl -G 'demo.live.local:8888/service-rocketmq/echo/hello?user=unit1
```