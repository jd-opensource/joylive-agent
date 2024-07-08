Quick Start
===

### 1. Quick Start

#### 1.1 Obtain the Agent Program

##### 1.1.1 Download the Binary Package

Download the latest binary package from the [Release](https://github.com/jd-opensource/joylive-agent/releases) page.

##### 1.1.2 Manual Compilation

> Please ensure you have a Maven build environment ready. Compile the project from the root directory using:
> ```bash
> mvn package -f pom.xml -DskipTests=true
> ```

- **Compile and obtain** the JoyLive Agent release package, located at: `joylive-package/target/live-x.x.x-SNAPSHOT`.

### 1.2 Obtain the Demo Program

Compile `joylive-agent` and obtain the binary packages of various projects under `joylive-demo/joylive-demo-springcloud3`.

- Gateway

  `joylive-demo-springcloud3-gateway` Spring Cloud Gateway demo

- Application

  `joylive-demo-springcloud3-provider` Spring Cloud application demo

### 1.3 Modify the Configuration

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
│   │   ├── ......
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
    │   ├── ......
    ├── spring
    │   ├── joylive-application-springboot2-1.0.0.jar
    │   ├── joylive-registry-springcloud3-1.0.0.jar
    │   ├── ......
    └── ......
```

- Modify the configuration files

The `config` directory contains agent configuration files and configuration files for multi-active traffic governance, microservice traffic governance, and lane strategies.

The default strategy loads from local files, but can be configured to load remotely.

| Location                   | Strategy Type     |
|----------------------------|-------------------|
| agent.sync.liveSpace       | Multi-active strategy    |
| agent.sync.microservice    | Microservice traffic governance strategy |
| agent.sync.laneSpace       | Lane strategy      |

In the strategy configuration items, `type` corresponds to the listener type, with `file` representing listening to local files.

- Modify environment variables

Common environment variables are listed below. For more details, please refer to the [Configuration Reference Manual](./config.md).

| **Name**                          | **Description**       | **Required** | **Default** | **Notes**                                                 |
|-----------------------------------|-----------------------|--------------|-------------|-----------------------------------------------------------|
| APPLICATION_NAME                  | Application name      | Yes          |             | It is recommended to be consistent with the Spring application name. |
| APPLICATION_SERVICE_NAME          | Service name          | No           | Application name | It is recommended to be consistent with the Spring Cloud application name. |
| APPLICATION_LOCATION_LIVESPACE_ID | Instance's multi-active space ID | Yes |             |                                                           |
| APPLICATION_LOCATION_UNIT         | Instance's unit code  | Yes          |             |                                                           |
| APPLICATION_LOCATION_CELL         | Instance's partition code | Yes      |             |                                                           |
| APPLICATION_LOCATION_LANESPACE_ID | Instance's lane space ID | No       |             | Configure when lane service is enabled.                   |
| APPLICATION_LOCATION_LANE         | Instance's lane code  | No           |             | Configure when lane service is enabled.                   |
| APPLICATION_LOCATION_REGION       | Instance's region     | No           |             |                                                           |
| APPLICATION_LOCATION_ZONE         | Instance's availability zone | No   |             |                                                           |
| CONFIG_LIVE_ENABLED               | Enable multi-active flow control | No | true       | Whether to enable multi-active flow control.              |
| CONFIG_POLICY_INITIALIZE_TIMEOUT  | Strategy synchronization timeout | No | 10000(ms)  |                                                           |
| CONFIG_FLOW_CONTROL_ENABLED       | Enable service flow control | No     | true       | Enable service flow control, including rate limiting, circuit breaking, load balancing, tag routing, etc. |
| CONFIG_LANE_ENABLED               | Enable lane flow control | No       | true       | Enable lane flow control.                                 |
| APPLICATION_SERVICE_GATEWAY       | Gateway type          | No           | NONE        | Set to FRONTEND for entry gateways, and NONE for regular applications. |

Note: When starting the `joylive-demo-springcloud3-gateway` Spring Cloud Gateway demo, set the gateway type to FRONTEND. For the `joylive-demo-springcloud3-provider` Spring Cloud application demo, the default is NONE.

### 1.4 Start the Gateway

In this example, environment variables are set instead of modifying configuration files.

> Note:
> - `${path_to_gateway_demo}` is the path to `joylive-demo-springcloud3-gateway.jar`.
> - `${path_to_agent}` is the path to `live.jar`.

To start a gateway instance in unit 1, use the following command:

```bash
# Set environment variables on Linux or macOS
export APPLICATION_NAME=springcloud3-gateway
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_SERVICE_GATEWAY=FRONTEND
# Set the Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-gateway.jar 

# Set environment variables on Windows (PowerShell)
$env:APPLICATION_NAME="springcloud3-gateway"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT="unit1"
$env:APPLICATION_LOCATION_CELL="cell1"
$env:APPLICATION_SERVICE_GATEWAY="FRONTEND"
# Set the Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-gateway.jar
```

### 1.5 Start the Application

Refer to the gateway startup process to simulate starting an application instance in unit 1 with the following command:

```bash
# Set environment variables on Linux or macOS
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
# Set the Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar 

# Set environment variables on Windows (PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT="unit1"
$env:APPLICATION_LOCATION_CELL="cell1"
# Set the Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

To simulate starting an application instance in unit 2, use the following command:

```bash
# Set environment variables on Linux or macOS
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=v4bEh4kd6Jvu5QBX09qYq-qlbcs
export APPLICATION_LOCATION_UNIT=unit2
export APPLICATION_LOCATION_CELL=cell4
# Set the Nacos access address
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Start
java -javaagent:${path_to_agent}/live.jar -jar ${path_to_provider_demo}/joylive-demo-springcloud3-provider.jar 

# Set environment variables on Windows (PowerShell)
$env:APPLICATION_NAME="springcloud3-provider"
$env:APPLICATION_LOCATION_LIVESPACE_ID="v4bEh4kd6Jvu5QBX09qYq-qlbcs"
$env:APPLICATION_LOCATION_UNIT="unit2"
$env:APPLICATION_LOCATION_CELL="cell4"
# Set the Nacos access address
$env:NACOS_ADDR="localhost:8848"
$env:NACOS_USERNAME="nacos"
$env:NACOS_PASSWORD="nacos"
# Start
java -javaagent:${path_to_agent}\live.jar -jar ${path_to_provider_demo}\joylive-demo-springcloud3-provider.jar
```

### 1.6 Verify Registration

Access the `Nacos` registration center and check the metadata of the service instances. The following data indicates that the agent enhancement was successful:

```properties
x-live-space-id=v4bEh4kd6Jvu5QBX09qYq-qlbcs
x-live-unit=unit1
x-live-cell=cell1
```

### 1.7 Traffic Testing

```bash
# Access the application interface through the gateway, specifying the unit variable unit1 to target unit1
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"

# Access the application interface through the gateway, specifying the unit variable unit2 to target unit2
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit2" -H "Host:demo.live.local"
```

## 2. Debugging

Debugging in an IDE, using IntelliJ IDEA as an example

### 2.1 Compile the Project

1. Compile and install the `joylive-agent` project using `mvn clean install`.
2. Obtain the full path of the compiled project output directory `target/live-${version}` from the `joylive-package`.

### 2.2 Start the Application

#### 2.2.1 Start the Registration Center

Prepare and start the `Nacos` registration center, and obtain its address, username, and password.

#### 2.2.2 Start the Gateway Project

Run the gateway application `joylive-demo-gateway`.

Configure parameters and environment variables.

![pic](./image/debug.png)

Add `-javaagent:live-${version} full path` to the VM options.

Refer to the following for configuring environment variables:

| Name | Value | Description                |
|----|---|-------------------|
| APPLICATION_LOCATION_CELL   | cell4  |  |
| APPLICATION_LOCATION_LIVESPACE_ID   | v4bEh4kd6Jvu5QBX09qYq-qlbcs  |                   |
| APPLICATION_LOCATION_UNIT   | unit2  |                   |
| APPLICATION_SERVICE_GATEWAY   | FRONTEND  |                   |
| CONFIG_LOCALHOST_ENABLED   | true  |                   |
| NACOS_ADDR   |   |                   |
| NACOS_NAMESPACE   | public  |                   |
| NACOS_PASSWORD   |   |                   |
| NACOS_USERNAME   |   |                   |

Refer to the `livespaces.json` configuration file in the `joylive-package` project for unit partition configuration.

#### 2.2.3 Start the Service Project

Run the microservice application `joylive-demo-provider`, referring to the gateway application's configuration.

Refer to the following for configuring environment variables:

| Name | Value                           | Description                |
|----|-----------------------------|-------------------|
| APPLICATION_LOCATION_CELL   | cell1                       |  |
| APPLICATION_LOCATION_LIVESPACE_ID   | v4bEh4kd6Jvu5QBX09qYq-qlbcs |                   |
| APPLICATION_LOCATION_UNIT   | unit1                       |                   |
| APPLICATION_SERVICE_GATEWAY   | FRONTEND                    |                   |
| CONFIG_LOCALHOST_ENABLED   | true                        |                   |
| NACOS_ADDR   |                             |                   |
| NACOS_NAMESPACE   | public                      |                   |
| NACOS_PASSWORD   |                             |                   |
| NACOS_USERNAME   |                             |                   |

### 2.3 Access Requests

```bash
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"
```

## 3. Supplement

### 3.1 Rocketmq

1. Prepare the Rocketmq environment and start the related services.
2. Refer to the above application configuration to prepare the runtime environment for `joylive-demo-rocketmq`. The following additional environment variables need to be configured:

| Variable | Name           | Description                             |
|----------|----------------|-----------------------------------------|
| CONFIG_LIVE_MQ_ENABLED   | Enable MQ Flag       | Whether to enable MQ<li>true Enable</li><li>Not Enable |
| CONFIG_LIVE_TOPICS   | Participating Multi-Active Topic Names     | Multiple topics separated by commas                      |
| ROCKETMQ_ADDR   | Rocketmq Service Address | IP:Port                          |

3. Refer to the above configuration to start `joylive-demo-springcloud3-gateway` for unit 1.
4. Refer to the above configuration to start `joylive-demo-springcloud3-provider` for unit 1.
5. Refer to the above configuration to start `joylive-demo-rocketmq` for unit 1.
6. Access verification
```shell
curl -G 'demo.live.local:8888/service-rocketmq/echo/hello?user=unit1
```
```