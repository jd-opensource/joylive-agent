# Release History

## 1.3.0
1. Support for local cloud priority during cell fault tolerance, reducing dedicated line bandwidth usage.
2. Circuit breaker and rate limiter, supporting the use of JsonPath to extract exception codes from response bodies.
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

   1. Microservices governance: Spring Cloud 3, Spring Gateway 3, Dubbo 2.6/2.7/3, SofaRpc.
   2. Trace propagation: Spring Cloud 3, Spring Gateway 3, Dubbo 2.6/2.7/3, SofaRpc, Grpc, RocketMQ 4/5, Kafka 3, HttpClient 3/4, HttpServlet, OkHttp 1/3, JDK HttpConnection, ThreadPool.
7. Released the accompanying cloud-native active-active controller.

## Limitations
1. Dynamic enhancement injection (agentmain) is not fully functional; please do not use it.
