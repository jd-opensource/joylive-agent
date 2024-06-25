# 发布历史

## 1.0.0

## 特性
1. 实现代理框架，包括微内核架构、类加载器隔离和插件管理等等。
2. 支持静态增强注入(premain)
3. 支持多活流量路由
4. 支持泳道流量路由
5. 支持微服务治理，包括集群重试策略，限流策略，负载均衡算法，标签路由策略，多活策略、泳道策略、优雅启动和下线。
6. 支持常用的框架，包括：

   1. 微服务治理：Spring cloud 3，Spring gateway 3，Dubbo 2.6/2.7/3，SofaRpc
   2. MQ路由：Rocketmq 4/5，Kafka 3
   3. 链路透传：Spring cloud 3，Spring gateway 3，Dubbo 2.6/2.7/3，SofaRpc，Grpc，Rocketmq 4/5，Kafka 3，Http client 3/4，Http servlet，Okhttp 1/3，JDK http connection，Thread pool。
7. 发布配套的云原生多活控制器

## 限制
1. 动态增强注入(agentmain)不完善，请不要使用
2. 用于泳道或者单元化的消息主题，会自动进行主题隔离，默认追加单元后缀“-unit-${unit}”或泳道后缀“-lane-${lane}”。另外增对MQ消费者，需要应用手动配置不同的消费组。

