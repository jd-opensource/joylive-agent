快速开始
===

## 开始

### 快速开始

#### 准备工作

> 请提前准备好maven编译环境，项目根目录执行命令编译: 
> ```bash
> mvn package -f pom.xml -DskipTests=true package
> ```

- **编译获取** JoyLive Agent Release包，路径：`joylive-package/target/live-x.x.x-SNAPSHOT`
- **编译获取** Demo二进制产物，路径：`joylive-demo/joylive-demo-springcloud3`下各项目的target目录内
- **下载获取** 并启动Nacos

#### 获取Demo二进制

- 网关

  `joylive-demo-springcloud3-gateway` Spring Cloud Gateway网关demo

- 应用

  `joylive-demo-springcloud3-provider` Spring Cloud应用demo

#### 修改Agent配置

JoyLive Agent包如下目录结构：

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

- 修改应用基础元数据

可以直接修改`config/bootstrap.properties`文件或者添加对应环境变量，环境变量信息如下：

| **类型** | **名称**                          | **说明**           | **必需** | **默认值** | **说明**                                                 |
| -------- | --------------------------------- | ------------------ | -------- | ---------- | -------------------------------------------------------- |
| 环境变量 | APPLICATION_NAME                  | 应用名             | 是       |            | 建议和Spring的应用名称保持一致                           |
| 环境变量 | APPLICATION_SERVICE_NAME          | 服务名             | 否       | 应用名称   | 建议和SpringCloud的应用名称保持一致                      |
| 环境变量 | APPLICATION_LOCATION_LIVESPACE_ID | 实例所在多活空间ID | 是       |            |                                                          |
| 环境变量 | APPLICATION_LOCATION_UNIT         | 实例所在单元编码   | 是       |            |                                                          |
| 环境变量 | APPLICATION_LOCATION_CELL         | 实例所在分区编码   | 是       |            |                                                          |
| 环境变量 | APPLICATION_LOCATION_LANESPACE_ID | 实例所在泳道空间ID | 否       |            | 当启用泳道服务时候配置                                   |
| 环境变量 | APPLICATION_LOCATION_LANE         | 实例所在泳道编码   | 否       |            | 当启用泳道服务时候配置                                   |
| 环境变量 | APPLICATION_LOCATION_REGION       | 实例所在地域       | 否       |            |                                                          |
| 环境变量 | APPLICATION_LOCATION_ZONE         | 实例所在可用区     | 否       |            |                                                          |
| 环境变量 | CONFIG_LIVE_ENABLED               | 启用多活流控       | 否       | true       | 是否要进行多活的流控                                     |
| 环境变量 | CONFIG_POLICY_INITIALIZE_TIMEOUT  | 策略同步超时       | 否       | 10000(ms)  |                                                          |
| 环境变量 | CONFIG_FLOW_CONTROL_ENABLED       | 启用服务流控       | 否       | true       | 启用服务流控，包括限流、熔断、负载均衡、标签路由等等策略 |
| 环境变量 | CONFIG_LANE_ENABLED               | 启用泳道流控       | 否       | true       | 启用泳道流控                                             |
| 环境变量 | APPLICATION_SERVICE_GATEWAY       | 网关类型           | 否       | NONE       | 若为入口网关设置为FRONTEND，普通应用设置为NONE           |

注意：启动`joylive-demo-springcloud3-gateway` Spring Cloud Gateway网关demo时需设置为FRONTEND。启动`joylive-demo-springcloud3-provider` Spring Cloud应用demo则不需要设置，默认为NONE。

- 修改策略同步

`config`目录下为agent配置相关文件以及多活流量治理，微服务流量治理，泳道策略配置文件。配置位置对应`config/config.yaml`配置文件中位置如下：

| 位置                    | 策略类型      |
| ----------------------- |-----------|
| agent.sync.liveSpace    | 多活流量策略    |
| agent.sync.microservice | 微服务流量治理策略 |
| agent.sync.laneSpace    | 泳道策略      |

策略配置项中`type`对应监听类型，file代表监听本地文件。

#### 启动网关

本例子中采用非修改配置文件而是设置环境变量方式。

> 说明：${path_to_gateway_demo}为joylive-demo-springcloud3-gateway demo下载所在路径；${path_to_agent}为joylive-agent下载解压后所在路径；

模拟单元1内启动网关实例，命令如下：

```bash
export APPLICATION_NAME=springcloud3-gateway
export APPLICATION_LOCATION_LIVESPACE_ID=6
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_SERVICE_GATEWAY=FRONTEND
# 设置启动的nacos访问地址
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Linux or macOS
java -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-gateway.jar -javaagent:${path_to_agent}/live.jar
# Windows
java -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-gateway.jar -javaagent:${path_to_agent}\live.jar
```

#### 启动应用

本例子中采用非修改配置文件而是设置环境变量方式。

> 说明：${path_to_provider_demo}为joylive-demo-springcloud3-provider demo下载所在路径；${path_to_agent}为joylive-agent下载解压后所在路径；

模拟单元1内启动应用实例，命令如下：

```bash
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=6
export APPLICATION_LOCATION_UNIT=unit1
export APPLICATION_LOCATION_CELL=cell1
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=production
# 设置启动的nacos访问地址
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Linux or macOS
java -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}/live.jar
# Windows
java -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}\live.jar
```

模拟单元2内启动应用实例，命令如下：

```bash
export APPLICATION_NAME=springcloud3-provider
export APPLICATION_LOCATION_LIVESPACE_ID=6
export APPLICATION_LOCATION_UNIT=unit2
export APPLICATION_LOCATION_CELL=cell4
export APPLICATION_LOCATION_LANESPACE_ID=1
export APPLICATION_LOCATION_LANE=beta
# 设置启动的nacos访问地址
export NACOS_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
# Linux or macOS
java -jar ${path_to_gateway_demo}/joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}/live.jar
# Windows
java -jar ${path_to_gateway_demo}\joylive-demo-springcloud3-provider.jar -javaagent:${path_to_agent}\live.jar
```

#### 效果验证

访问nacos注册中心，检查服务实例的元数据有如下数据代表agent增强成功。

```properties
unit=unit1
laneSpaceId=1
liveSpaceId=6
cell=cell1
lane=production
```

#### 流量测试

```bash
# 通过网关访问应用接口，指定单元变量unit1，指向访问unit1单元
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit1" -H "Host:demo.live.local"

# 通过网关访问应用接口，指定单元变量unit2，指向访问unit2单元
curl -X GET "http://localhost:8888/service-provider/echo/abc?user=unit2" -H "Host:demo.live.local"
```
