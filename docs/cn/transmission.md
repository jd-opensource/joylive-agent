链路透传
===

在多活和泳道场景，需要透传多活的单元信息和泳道信息，让流量尽量闭环在单元或泳道内

## 1. 请求上下文

请求上下文`RequestContext`通过线程变量来携带信息`Carrier`

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
+ copyAttribute(Attributes) void
}

class Carrier {
<<Interface>>
+ getCargos() Collection~Cargo~
+ getCargo(String) Cargo
+ addCargo(CargoRequire, Iterable~T~, Function~T, String~, Function~T, String~) void
+ addCargo(CargoRequire, Enumeration~String~, Function~String, Enumeration~String~~) void
+ addCargo(Cargo) void
+ addCargo(CargoRequire, Map~String, Collection~String~~) void
+ addCargo(CargoRequire, Iterable~String~, Function~String, List~String~~) void
+ addCargo(CargoRequire, M, Function~String, Collection~String~~) void
+ addCargo(String, String) void
+ setCargo(String, String) void
+ removeCargo(String) void
+ cargos(BiConsumer~String, String~) void
+ cargos(Consumer~Cargo~) void
}

class Cargo {
+ add(String) void
+ add(Collection~String~) void
+ add(Enumeration~String~) void
+ toString() String
}

class Tag {
# setKey(String) void
+ getValues() List~String~
+ getFirstValue() String
+ getKey() String
# add(Enumeration~String~) void
+ getValue() String
# add(Collection~String~) void
+ toString() String
# add(String) void
# setValues(List~String~) void
}

Cargo  -->  Tag
Carrier  -->  Attributes
RequestContext ..> Carrier
Carrier ..> Cargo

```
## 2. 定义需要透传的信息

基于扩展的方式来定义需还原到上下文的透传信息

```mermaid
classDiagram
direction BT
class CargoRequire {
<<Interface>>

}
class CargoRequires
class LaneCargoRequire
class LiveCargoRequire

CargoRequires  ..>  CargoRequire 
LaneCargoRequire  ..>  CargoRequire 
LiveCargoRequire  ..>  CargoRequire 

```
1. `CargoRequire`定义需要透传的信息，并且描述为扩展接口
2. `LiveCargoRequire`是系统内置的多活透传定义实现
3. `LaneCargoRequire`是系统内置的多泳道透传定义实现
4. `CargoRequires`用于包装聚合所有的`CargoRequire`实现

| 键               | 说明         |
|-----------------|------------|
| x-live-space-id | 多活空间ID     |
| x-live-rule-id  | 多活空间路由ID   |
| x-live-uid      | 多活空间路由变量   |
| x-live-         | 多活前缀匹配     |
| x-lane-space-id | 泳道空间ID     |
| x-lane-code     | 泳道         |

## 3. 透传实现

在服务调用方，拦截方法请求，把需要透传的上下文变量设置到传输对象里面。在服务提供方法，拦截请求处理，把透传的信息还原到上下文。

1. HTTP调用使用Header来传递
2. RPC调用使用Attachment来携带
3. MQ调用使用Attachment来携带

下面以Dubbo3为例来说明透传的实现

### 3.1 消费者

#### 3.1.1 消费者插件定义

```java
@Extension(value = "DubboConsumerDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@Injectable
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(DubboConsumerDefinition.TYPE_CONSUMER_CONTEXT_FILTER)
public class DubboConsumerDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_CONSUMER_CONTEXT_FILTER = "org.apache.dubbo.rpc.cluster.filter.support.ConsumerContextFilter";

    private static final String METHOD_INVOKE = "invoke";

    protected static final String[] ARGUMENT_INVOKE = new String[]{
            "org.apache.dubbo.rpc.Invoker",
            "org.apache.dubbo.rpc.Invocation"
    };

    @Inject
    private List<CargoRequire> requires;

    public DubboConsumerDefinition() {

        this.matcher = () -> MatcherBuilder.named(TYPE_CONSUMER_CONTEXT_FILTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INVOKE).
                                and(MatcherBuilder.arguments(ARGUMENT_INVOKE)),
                        () -> new DubboConsumerInterceptor(requires))};
    }
}
```

该插件定义描述了拦截类型`org.apache.dubbo.rpc.cluster.filter.support.ConsumerContextFilter`的方法`invoke`

#### 3.1.1 消费者拦截器

```java
public class DubboConsumerInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public DubboConsumerInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        attachTag((RpcInvocation) ctx.getArguments()[1]);
    }

    private void attachTag(RpcInvocation invocation) {
        Carrier carrier = RequestContext.getOrCreate();
        carrier.cargos(tag -> invocation.setAttachment(tag.getKey(), tag.getValue()));
        carrier.addCargo(require, RpcContext.getClientAttachment().getObjectAttachments(), Label::parseValue);
    }

}
```

该拦截器在方法进入前，遍历所有`Cargo`对象，设置成为请求的附件，同时从请求里面还原应用程序设置的透传信息到上下文

### 3.2 服务提供者

#### 3.2.1 服务提供者插件定义

```java
@Injectable
@Extension(value = "DubboProviderDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(DubboConsumerDefinition.TYPE_CONSUMER_CONTEXT_FILTER)
@ConditionalOnClass(DubboProviderDefinition.TYPE_CONTEXT_FILTER)
public class DubboProviderDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_CONTEXT_FILTER = "org.apache.dubbo.rpc.filter.ContextFilter";

    private static final String METHOD_INVOKE = "invoke";

    @Inject
    private List<CargoRequire> requires;

    public DubboProviderDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_CONTEXT_FILTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INVOKE).
                                and(MatcherBuilder.arguments(ARGUMENT_INVOKE)),
                        () -> new DubboProviderInterceptor(requires))};
    }
}
```

该插件定义描述了拦截类型`org.apache.dubbo.rpc.filter.ContextFilter`的方法`Invoke`

#### 3.2.1 服务提供者拦截器

```java
public class DubboProviderInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public DubboProviderInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        restoreTag((RpcInvocation) ctx.getArguments()[1]);
    }

    private void restoreTag(RpcInvocation invocation) {
        RequestContext.create().addCargo(require, invocation.getObjectAttachments(), Label::parseValue);
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        RequestContext.remove();
    }

}

```
该拦截器在方法进入的时候，从请求里面还原需要的上下文透传信息，同时在链处理完后删除上下文。
