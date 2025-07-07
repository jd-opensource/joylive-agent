Link Transmission
===

In multi-live and lane scenarios, it is necessary to transmit multi-live unit information and lane information to ensure that traffic remains within the unit or lane as much as possible.

## 1. Request Context

The request context `RequestContext` carries information through thread variables `Carrier`.

```mermaid
classDiagram
    direction BT
    class RequestContext {
        + get() Carrier
        + getOrCreate() Carrier
        + set(Carrier) void
        + create() Carrier
        + remove() void
        + getCargo(String) Cargo?
        + hasCargo() boolean
        + cargos(Consumer~Cargo~) void
        + cargos(BiConsumer~String, String~) void
        + getAttribute(String) T?
        + setAttribute(String, Object) void
        + removeAttribute(String) T?
        + isTimeout() boolean
    }

    class Attributes {
        <<Interface>>
        + removeAttribute(String) T
        + hasAttribute(String) boolean
        + setAttribute(String, Object) void
        + attributes(BiConsumer~String, Object~) void
        + getAttribute(String) T
    }

    class Carrier {
        <<Interface>>
        + getCargos() Collection~Cargo~
        + getCargo(String) Cargo
        + addCargo(Cargo) void
        + addCargo(String, String) void
        + addCargo(Predicate~String~, M, Function~String,Collection~ func) void
        + setCargo(String, String) void
        + removeCargo(String) void
        + cargos(BiConsumer~String, String~) void
        + cargos(Consumer~Cargo~) void
    }

    class Cargo {
        + add(String) void
        + add(Collection~String~) void
    }

    class Label {
        <<Interface>>
        + getKey() String
        + getFirstValue() String
        + getValues() List~String~
        + getValue() String
    }

    class Tag {

    }
    Tag --> Label
    Cargo --> Tag
    Carrier --> Attributes
    RequestContext ..> Carrier
    Carrier ..> Cargo

```
## 2. Define Information to be Transmitted

Define the information to be restored to the context based on extensions.

```mermaid
classDiagram
    direction BT
    class CargoRequire {
        <<Interface>>
        getNames() Set~String~
        getPrefixes() Set~String~
        test(String) boolean
    }
    class CargoRequires
    class LiveCargoRequire

    CargoRequires  -->   CargoRequire
    LiveCargoRequire  -->   CargoRequire

```
1. `CargoRequire` defines the information to be transmitted and describes it as an extension interface.
2. `LiveCargoRequire` is a built-in implementation of multi-live transmission definition.
3. `CargoRequires` is used to aggregate all `CargoRequire` implementations.

| Key               | Description         |
|-------------------|---------------------|
| x-live-space-id   | Multi-live space ID |
| x-live-rule-id    | Multi-live space route ID |
| x-live-uid        | Multi-live space route variable |
| x-live-           | Multi-live prefix match |
| x-lane-space-id   | Lane space ID       |
| x-lane-code       | Lane code           |

## 3. Propagation Methods

Supports multiple propagation methods implemented through extensions. Default is W3C propagation while retaining legacy Live propagation.

```mermaid
classDiagram
    direction BT
    class Propagation {
        <<Interface>>
        + write(HeaderWriter) void
        + write(Carrier, HeaderWriter) void
        + write(Carrier, Location, HeaderWriter) void
        + write(HeaderReader, HeaderWriter) void
        + write(Carrier, HeaderReader) void
    }
    
    class AutoPropagation {
        -Collection~Propagation~ readers
        -Propagation writer
        -AutoDetect autoDetect
    }
    
    class AutoDetect{
        <<enumeration>>
        NONE
        FIRST
        ALL
    }

    AutoPropagation --> Propagation
    W3cPropagation --> Propagation
    LivePropagation --> Propagation
    AutoPropagation ..> AutoDetect
```

1. `W3cPropagation` follows W3C standard specifications for propagation
2. `LivePropagation` follows Live specifications for propagation (one Key corresponds to one Header)
3. `AutoPropagation` used for automatic detection of propagation methods

## 4. Transmission Implementation

On the service caller side, intercept the method request and set the context variables that need to be transmitted into the transmission object. On the service provider side, intercept the request processing and restore the transmitted information into the context.

1. HTTP calls use Headers for transmission.
2. RPC calls use Attachments for transmission.
3. MQ calls use Attachments for transmission.

Below is an example of the transmission implementation using Dubbo3.

### 4.1 Consumer

#### 4.1.1 Consumer Plugin Definition

```java
@Extension(value = "DubboConsumerDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@Injectable
@ConditionalOnDubbo3TransmissionEnabled
@ConditionalOnClass(DubboConsumerDefinition.TYPE_ABSTRACT_CLUSTER_INVOKER)
public class DubboConsumerDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_ABSTRACT_CLUSTER_INVOKER = "org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker";

    private static final String METHOD_INVOKE = "invoke";

    protected static final String[] ARGUMENT_INVOKE = new String[]{
            "org.apache.dubbo.rpc.Invocation"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public DubboConsumerDefinition() {

        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_ABSTRACT_CLUSTER_INVOKER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INVOKE).
                                and(MatcherBuilder.arguments(ARGUMENT_INVOKE)),
                        () -> new DubboConsumerInterceptor(propagation))};
    }
}
```

This plugin definition describes intercepting the method `invoke` of the type `org.apache.dubbo.rpc.cluster.filter.support.ConsumerContextFilter`.

#### 4.1.2 Consumer Interceptor

```java
public class DubboConsumerInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public DubboConsumerInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RpcInvocation invocation = ctx.getArgument(0);
        Carrier carrier = RequestContext.getOrCreate();
        // read from rpc context by live propagation
        LIVE_PROPAGATION.read(carrier, new ObjectMapReader(RpcContext.getClientAttachment().getObjectAttachments()));
        // write to invocation with live attachments in rpc context
        propagation.write(carrier, new ObjectMapWriter(invocation.getObjectAttachments(), invocation::setAttachment));
        ServiceMetadata serviceMetadata = invocation.getServiceModel().getServiceMetadata();
        String provider = (String) serviceMetadata.getAttachments().get(PROVIDED_BY);
        if (provider != null && !provider.isEmpty()) {
            invocation.setAttachmentIfAbsent(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);
        }
    }
}
```

This interceptor, before entering the method, iterates over all `Cargo` objects, setting them as attachments to the request. It also restores the application-set transmission information from the request into the context.

### 4.2 Service Provider

#### 4.2.1 Service Provider Plugin Definition

```java
@Injectable
@Extension(value = "DubboProviderDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnDubbo3TransmissionEnabled
@ConditionalOnClass(DubboProviderDefinition.TYPE_CONTEXT_FILTER)
public class DubboProviderDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_CONTEXT_FILTER = "org.apache.dubbo.rpc.filter.ContextFilter";

    private static final String METHOD_INVOKE = "invoke";

    protected static final String[] ARGUMENT_INVOKE = new String[]{
            "org.apache.dubbo.rpc.Invoker",
            "org.apache.dubbo.rpc.Invocation"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public DubboProviderDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_CONTEXT_FILTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INVOKE).
                                and(MatcherBuilder.arguments(ARGUMENT_INVOKE)),
                        () -> new DubboProviderInterceptor(propagation))};
    }
}
```

This plugin definition describes intercepting the method `invoke` of the type `org.apache.dubbo.rpc.filter.ContextFilter`.

#### 4.2.2 Service Provider Interceptor

```java
public class DubboProviderInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public DubboProviderInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RpcInvocation invocation = ctx.getArgument(1);
        propagation.read(RequestContext.create(), new ObjectMapReader(invocation.getObjectAttachments()));
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        RequestContext.remove();
    }

}
```

This interceptor restores the necessary context transmission information from the request when entering the method and removes the context after the chain processing is completed.

## 4. Propagation Configuration

The `transmitConfig` field defined in `GovernanceConfig` reads configuration information from `config.yaml` as shown below:

```yaml
agent:
  governance:
    transmission:
      type: ${CONFIG_TRANSMISSION_TYPE:live}
      autoDetect: ${CONFIG_TRANSMISSION_AUTO_DETECT:NONE}
      keys:
        - x-live-space-id
        - x-live-rule-id
        - x-live-uid
        - x-lane-space-id
        - x-lane-code
      prefixes:
        - x-live-
        - x-lane-
        - x-service-
      thread:
        excludeExecutors:
          - io.netty.channel.MultithreadEventLoopGroup
          - io.netty.channel.nio.NioEventLoop
          - io.netty.channel.SingleThreadEventLoop
          - io.netty.channel.kqueue.KQueueEventLoopGroup
          - io.netty.channel.kqueue.KQueueEventLoop
          - org.apache.tomcat.util.threads.ThreadPoolExecutor
          - org.apache.tomcat.util.threads.ScheduledThreadPoolExecutor
          - org.apache.tomcat.util.threads.InlineExecutorService
          - javax.management.NotificationBroadcasterSupport$1
          - com.netflix.stats.distribution.DataPublisher$PublishThreadFactory
          - com.alibaba.druid.pool.DruidAbstractDataSource$SynchronousExecutor
        excludeTasks:
          - com.jd.live.agent.core.thread.NamedThreadFactory
          - com.jd.jr.sgm.client.disruptor.LogEventFactory
          - com.jd.jr.sgm.client.util.AgentThreadFactory
          - com.jd.pfinder.profiler.common.util.NamedThreadFactory
          - io.opentelemetry.sdk.internal.DaemonThreadFactory
          - io.sermant.dubbo.registry.factory.RegistryNotifyThreadFactory
          - io.sermant.dynamic.config.init.DynamicConfigThreadFactory
          - io.sermant.flowcontrol.common.factory.FlowControlThreadFactory
          - io.sermant.loadbalancer.factory.LoadbalancerThreadFactory
          - io.sermant.core.utils.ThreadFactoryUtils
          - io.sermant.implement.service.xds.handler.XdsHandler.NamedThreadFactory
          - io.sermant.discovery.factory.RealmServiceThreadFactory
          - org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory
          - sun.rmi.runtime.RuntimeUtil$1
          - sun.rmi.transport.tcp.TCPTransport$1
          - sun.rmi.transport.DGCImpl$1
          - sun.rmi.transport.DGCAckHandler$1
          - org.apache.tomcat.util.threads.TaskThreadFactory
        excludeExecutorPrefixes:
          - com.jd.live.agent.shaded.
          - com.netflix.hystrix.util.HystrixTimer$ScheduledExecutor$
          - com.netflix.stats.distribution.DataPublisher$PublishThreadFactory$
          - com.alibaba.nacos.
        excludeTaskPrefixes:
          - reactor.core.scheduler.BoundedElasticScheduler$$Lambda
          - org.springframework.cloud.commons.util.InetUtils$$Lambda$
          - com.alibaba.nacos.
          - com.netflix.discovery.
          - com.jd.live.agent.shaded.
          - org.apache.catalina.core.ContainerBase$
          - org.apache.catalina.core.StandardServer$$Lambda$
          - com.netflix.loadbalancer.PollingServerListUpdater$
          - com.netflix.hystrix.util.HystrixTimer$
          - com.netflix.servo.util.ExpiringCache$
          - com.zaxxer.hikari.pool.HikariPool$
```