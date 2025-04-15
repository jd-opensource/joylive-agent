# Release History

## 1.7.0-SNAPSHOT

1. Added support for Spring Cloud Greenwich, currently supporting Spring Cloud Greenwich/Hoxton/2020/2021/2022/2023/2024.
2. Added support for Apache HttpClient governance with Spring Cloud Zuul
3. Added support for registering Spring Boot applications, converting them into microservices.
4. Added support for Eureka service registry
5. Added support for multi-registry registration and subscription.
6. Added support for maximum circuit breaker instance ratio
7. Improved dynamic configuration injection, supporting injection of fields in objects annotated with @ConfigurationProperties.
8. Fixed other issues and improved stability.

## 1.6.0

1. Added support for Spring Cloud 2024. Currently, Spring Cloud Hoxton/2020/2021/2022/2023/2024 are supported.
2. Added support for retry and circuit breaker for Spring Cloud gRPC outbound traffic. Currently, full support for gRPC unary mode governance is provided.
3. Added support for Spring Cloud client to obtain server exceptions, facilitating retry and circuit breaker.
4. Added support for Spring Cloud port routing.
5. Added support for Spring Cloud integration with nacos configuration center, supporting dynamic modification of slf4j log levels and fields annotated with @Value.
6. Added support for Redis cluster-level rate limiting.
7. Added support for W3C Baggage propagation.
8. Added support for gradual recovery of circuit breaker traffic. 
9. Improved extension conditional matching, supporting custom combination conditions and simplifying related configurations. 
10. Improved label matching logic in flow control. 
11. Provided context lock to facilitate the use of interceptors. 
12. Modified configuration to not start multi-active, swimlane, and flow control governance by default. These can be enabled through environment variable configuration. 
13. Fixed an issue where configuring the Agent in JAVA_TOOL_OPTIONS would affect the use of Java operation and maintenance tools. Added support for configuring to filter out related applications. 
14. Fixed a series of issues and improved stability. 
15. Optimized performance and startup speed.

## 1.5.2

1. Hot fix for policy timed update

## 1.5.1

1. Hot fix for swimlane policy

## 1.5.0

1. Added support for routing governance of outbound traffic from Spring Cloud gRPC
2. Added support for synchronizing policies from Nacos configuration center
3. Added support for Spring Cloud Hoxton & 2020 & 2022
4. Improved swimlane routing
5. Added support for configuring write method prefixes in service multi-active strategies
6. Added support for Fastjson2 as a JSON and JsonPath parser
7. Fixed the issue of missing circuit breaker metrics
8. Fixed the problem of stack overflow when getting weights
9. Fixed other issues

## 1.4.0

1. Supports Spring Cloud gRPC inbound traffic governance
2. Support for cluster priority routing
3. InboundFilter has been changed to asynchronous mode
4. Support for using JsonPath or ValuePath to get error codes
5. Optimized liveless service routing
6. Improved group routing
7. Retry strategy now includes method matching conditions
8. Support for disabling conflicting Spring Cloud instance providers
9. Support for token authentication
10. Support for leaky bucket and smooth warmup token bucket rate limiting algorithms
11. Support for rate limiting based on system load
12. RPC now also supports Query and Cookie matching for hot parameter rate limiting
13. Service registration now includes warm-up parameters, including startup time, weight, and warm-up time 
14. Fixed the issue of not being able to return non-standard HTTP response codes 
15. Fixed routing issues under multiple spaces 
16. Fixed the issue where retry did not handle wrapped exceptions properly 
17. Fixed the notification problem of routing 
18. Fixed the issue of getting response body when Spring Cloud Gateway is in circuit breaking mode 
19. Fixed the case sensitivity issue with HTTP Header and Cookie names.

## 1.3.3

1. Fixed the problem that bytecode interceptors did not throw exceptions 

## 1.3.2

1. Fix a Null Pointer Exception (NPE).

## 1.3.1
1. Fix the default retry policy from spring gateway request.
2. Support spring cloud gateway only enable multi-live and lane feature.
3. Add auth and permission policy.
4. Fix FailsafeClusterInvoker response.
5. Fix circuit breaker for reactive.
6. Fix Unready inbound filter error.
7. Fixing the lack of unified handling for live exceptions.

## 1.3.0
1. Support for local cloud priority during cell fault tolerance, reducing dedicated line bandwidth usage.
2. Circuit breaker and retry, supporting the use of JsonPath to extract exception codes from response bodies.
3. Support for adaptive load balancing algorithms.
4. Add accessMode in CellRoute, ensuring that business cell failures do not affect other businesses.
5. RouteFilter and OutboundFilter have been split.
6. Rejection types have been refined, making it easier to collect circuit breaker and rate limiter data.
7. Added a demo application for SofaRpc.
8. Fixed issues in SofaRpc.
9. Fixed issues with tag routing.
10. Fixed policy synchronization problems.
11. Fixed other issues and improved stability.

## 1.2.0

1. Add spring cloud 2023 support.
2. Add auth inbound filter
3. Fixed some general bugs and improved stability.

## 1.1.0

1. Add complete circuit-break policy support.
2. Add OutboundListener support to refine more governance enhancement pointcuts.
3. Add rabbitmq, pulsar tag transmission support.
4. Fixed some general bugs and improved stability.

## 1.0.0

## Features
1. Implemented the proxy framework, including micro-kernel architecture, class loader isolation, and plugin management, among others.
2. Supported static enhancement injection (premain).
3. Supported active-active traffic routing.
4. Supported lane-based traffic routing.
5. Supported microservices governance, including cluster retry strategies, rate limiting strategies, load balancing algorithms, tag-based routing strategies, active-active strategies, lane strategies, graceful startup, and shutdown.
6. Supported common frameworks, including:
   - Microservices governance: Spring Cloud 3, Spring Gateway 3, Dubbo 2.6/2.7/3, SofaRpc.
   - Trace propagation: Spring Cloud 3, Spring Gateway 3, Dubbo 2.6/2.7/3, SofaRpc, Grpc, RocketMQ 4/5, Kafka 3, HttpClient 3/4, HttpServlet, OkHttp 1/3, JDK HttpConnection, ThreadPool.
7. Released the accompanying cloud-native active-active controller.

## Limitations
1. Dynamic enhancement injection (agentmain) is not fully functional; please do not use it.
