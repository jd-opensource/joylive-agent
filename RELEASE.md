# Release History

## 1.0.0

### Features
1. Implemented the proxy framework, including microkernel architecture, classloader isolation, and plugin management, among other features.
2. Supported static enhancement injection (premain).
3. Supported multi-active traffic routing.
4. Supported lane-based traffic routing.
5. Supported microservice governance, including cluster retry strategies, rate limiting strategies, load balancing algorithms, tag-based routing strategies, multi-active strategies, lane strategies, graceful startup, and shutdown.
6. Supported common frameworks, including:

    1. Microservice Governance: Spring Cloud 3, Spring Gateway 3, Dubbo 2.6/2.7/3, SofaRpc.
    2. Context Propagation: Spring Cloud 3, Spring Gateway 3, Dubbo 2.6/2.7/3, SofaRpc, Grpc, RocketMQ 4/5, Kafka, Http Client 3/4, Http Servlet, OkHttp 1/3, JDK Http Connection, Thread Pool.
7. Released a cloud-native multi-active controller.

### Limitations
1. Dynamic enhancement injection (agentmain) is not fully functional; please do not use it.
