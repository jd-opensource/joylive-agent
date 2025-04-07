# 发布历史

## 1.7.0-SNAPSHOT

1. 新增支持 Spring cloud greenwich，当前已经支持Spring cloud Greenwich/Hoxton/2020/2021/2022/2023/2024
2. 新增支持 Spring cloud zuul 的Apache HttpClient治理
3. 新增支持多注册中心注册
4. 新增支持 Spring boot 应用的注册，转换成微服务应用
5. 新增支持最大实例级熔断比例
6. 完善动态配置注入，支持对带有@ConfigurationProperties注解的对象的字段进行注入
7. 修复其它问题和稳定性提升

## 1.6.0

1. 新增支持 Spring cloud 2024，当前已经支持Spring cloud Hoxton/2020/2021/2022/2023/2024
2. 新增支持 Spring cloud grpc 出流量的重试和熔断，当前已经完整支持grpc unary模式的治理
3. 新增支持 Spring cloud 客户端获取到服务端的异常，便于重试和熔断 
4. 新增支持 Spring cloud 端口路由
5. 新增支持 Spring cloud 集成Nacos配置中心，支持对slf4j的日志级别和@Value注解的字段进行动态修改
6. 新增支持 Redis 集群级限流 
7. 新增支持 W3c Baggage 透传
8. 新增支持熔断的流量渐进性恢复 
9. 完善扩展的条件匹配，支持自定义组合条件，简化了相关配置 
10. 完善流控中的标签匹配逻辑 
11. 提供上下文锁，便于拦截器使用 
12. 修改配置，默认不启动多活、泳道和流控的治理，通过环境变量配置开启 
13. 修复当在JAVA_TOOL_OPTIONS中配置Agent的时候，会影响Java运维工具使用的问题，支持配置过滤掉相关应用 
14. 修复一系列问题，提升稳定性 
15. 优化性能和启动速度

## 1.5.2

1. 策略定时更新的热修复

## 1.5.1

1. 针对泳道策略的热修复

## 1.5.0

1. 支持spring cloud grpc 出流量的路由治理
2. 支持从Nacos配置中心同步策略
3. 支持Spring cloud Hoxton & 2020 & 2022
4. 完善泳道路由
5. 支持服务多活策略中的写方法前缀配置
6. 支持Fastjson2作为Json和JsonPath的解析器
7. 修复缺乏熔断指标的问题
8. 修复获取权重堆栈溢出问题
9. 修复其它问题

## 1.4.0

1. 支持 spring cloud grpc 入流量治理；
2. 支持集群优先路由；
3. InboundFilter改成异步模式；
4. 支持使用JsonPath或ValuePath来获取异常码；
5. 优化非多活服务路由；
6. 支持分组路由；
7. 重试策略增加方法匹配条件； 
8. 支持禁用冲突的某些Spring Cloud的实例提供者；
9. 支持令牌认证；
10. 支持漏桶和平滑预热令牌桶限流算法； 
11. 支持根据系统负载限流；
12. RPC也支持Query和Cookie匹配，便于支持热点参数限流；
13. 服务注册增加预热参数，包括启动时间，权重和预热时间； 
14. 修复不能返回非标准HTTP应答码的问题； 
15. 修复多个空间下的路由问题； 
16. 修复重试没有处理好包装异常的问题； 
17. 修复路由的通知问题； 
18. 修复Spring Cloud Gateway 熔断时候获取应答体的问题； 
19. 修复Http的Header和Cookie大小敏感问题。

## 1.3.3

1. 修复字节码拦截器没有抛出异常的问题。

## 1.3.2

1. 修复认证过滤器空指针问题。

## 1.3.1
1. 修复Spring Gateway请求的默认重试策略。
2. 支持Spring Cloud Gateway仅启用多活和泳道功能。
3. 添加认证和权限策略。
4. 修复FailsafeClusterInvoker响应。
5. 修复反应式的断路器问题。
6. 修复UnreadyInboundFilter过滤链调用错误。
7. 修复对实时异常缺乏统一处理的问题。

## 1.3.0
1. 在分区容错的时候支持本云优先，减少专线带宽
2. 熔断和重试，支持采用JsonPath从应答体中提取异常码
3. 支持自适应负载均衡算法
4. 在分区路由上增加了访问模式，使业务的分区故障切换不影响其它业务
5. 拆分了RouteFilter和OutboundFilter
6. 细化了拒绝类型，方便统计熔断限流数据
7. 增加了SofaRpc演示应用
8. 修复SofaRpc异常
9. 修复标签路由问题 
10. 修复策略同步问题
11. 修复其它问题和稳定性提升.

## 1.2.0

1. 支持spring cloud 2023.
2. 支持认证策略.
3. 修复通用问题和稳定性提升.

## 1.1.0

1. 支持完整的服务熔断策略.
2. 增加了出流量监听器，重构了治理增强切面.
3. 支持rabbitmq和pulsar链路透传.
4. 修复通用问题和稳定性提升.

## 1.0.0

## 特性
1. 实现代理框架，包括微内核架构、类加载器隔离和插件管理等等。
2. 支持静态增强注入(premain)
3. 支持多活流量路由
4. 支持泳道流量路由
5. 支持微服务治理，包括集群重试策略，限流策略，负载均衡算法，标签路由策略，多活策略、泳道策略、优雅启动和下线。
6. 支持常用的框架，包括：
   - 微服务治理：Spring cloud 3，Spring gateway 3，Dubbo 2.6/2.7/3，SofaRpc
   - 链路透传：Spring cloud 3，Spring gateway 3，Dubbo 2.6/2.7/3，SofaRpc，Grpc，Rocketmq 4/5，Kafka 3，Http client 3/4，Http servlet，Okhttp 1/3，JDK http connection，Thread pool。
7. 发布配套的云原生多活控制器

## 限制
1. 动态增强注入(agentmain)不完善，请不要使用

