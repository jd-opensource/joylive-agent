扩展体系
===

基于Java SPI机制，提供了注解、依赖注入和配置功能

## 1. 定义扩展

定义扩展接口，并使用`@Extensible`注解来进行扩展的声明。

如下定义了一个`AgentService`的扩展接口

```java
@Extensible("AgentService")
public interface AgentService {
    
    CompletableFuture<Void> start();
    
    CompletableFuture<Void> stop();
    
}
```

## 2. 扩展实现

实现扩展接口，并使用`@Extension`注解来进行扩展实现的声明。

如下实现了文件同步服务`LiveSpaceFileSyncer`

```java
@Injectable
@Extension("LiveSpaceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "file")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveSpaceFileSyncer extends AbstractFileSyncer<List<LiveSpace>> {
    
    private static final String CONFIG_LIVE_SPACE = "livespaces.json";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private SyncConfig syncConfig = new SyncConfig();
}
```
该类上的注解如下：
1. `@Extension`注解声明扩展实现，并提供了名称
2. `@ConditionalOnProperty`注解声明启用的条件，可以组合多个条件
3. `@Injectable`注解声明类型需要启用自动注入
4. `@Inject`注解字段需要启用自动注入
5. `@Config`注解字段需要启用自动配置

## 3. 启用扩展
1. 在SPI文件`META-INF/services/com.jd.live.agent.core.service.AgentService`中配置扩展全路径名
`com.jd.live.agent.implement.service.policy.file.LiveSpaceFileSyncer`
2. 配置多活空间同步类型为`file`

## 4. 注解说明
### 4.1 `@Extensible`

该注解声明在扩展接口上，其配置参数如下

| 参数             | 类型                   | 默认值  | 说明      |
|----------------|----------------------|------|---------|
| value          | String               |      | 扩展接口名称  |

### 4.2 `@Extension`
该注解声明在扩展实现上，其配置参数如下

| 参数    | 类型      | 默认值 | 说明           |
|-------|---------|-----|--------------|
| value | String  |     | 扩展实现名称       |
| order | int     | Short.MAX_VALUE | 优先级，值越小优先级越高 |
| singleton | boolean | ture| 是否单例         |

### 4.3 Enablement Conditions
Extension implementations can configure conditional switches

#### 4.3.1 `@ConditionalOnProperty`
This annotation declares configuration property switches for enablement. Multiple `@ConditionalOnProperty` can be configured with AND relationship between them.

| Parameter       | Type                  | Default | Description                                   |
|-----------------|-----------------------|---------|-----------------------------------------------|
| value           | String                |         | Configuration value                          |
| name            | String[]              |         | Configuration name array                     |
| matchIfMissing  | boolean               | true    | Whether to match when configuration is missing |
| relation        | ConditionalRelation   | OR      | Relationship<br/>OR or<br/>AND and           |
| caseSensitive   | boolean               | false   | Case sensitivity                             |
| comparison      | enum                  | EQUAL   | Comparison<br/>EQUAL equals<br/>NOT_EQUAL not equals |

#### 4.3.2 `@ConditionalOnProperties`
This annotation declares multiple configuration property switches, composed of multiple `@ConditionalOnProperty` with defined relationships.

| Parameter | Type                      | Default | Description                      |
|-----------|---------------------------|---------|----------------------------------|
| value     | ConditionalOnProperty[]    |         | Configuration item array         |
| relation  | ConditionalRelation        | AND     | Relationship<br/>OR or<br/>AND and |

#### 4.3.3 `@ConditionalOnClass`
This annotation declares type conditions for enablement. Matches if the type exists. Multiple `@ConditionalOnClass` can be configured with AND relationship.

| Parameter | Type      | Default | Description       |
|-----------|-----------|---------|-------------------|
| value     | String    |         | Fully-qualified class name |

#### 4.3.4 `@ConditionalOnClasses`
This annotation declares multiple type conditions, composed of multiple `@ConditionalOnClass` with AND relationship.

| Parameter | Type                   | Default | Description       |
|-----------|------------------------|---------|-------------------|
| value     | ConditionalOnClass[]   |         | Type condition array |

#### 4.3.5 `@ConditionalOnMissingClass`
This annotation declares type conditions for enablement. Matches if the type doesn't exist. Multiple `@ConditionalOnMissingClass` can be configured with AND relationship.

| Parameter | Type      | Default | Description       |
|-----------|-----------|---------|-------------------|
| value     | String    |         | Fully-qualified class name |

#### 4.3.6 `@ConditionalOnMissingClasses`
This annotation declares multiple type conditions, composed of multiple `@ConditionalOnMissingClass` with AND relationship.

| Parameter | Type                          | Default | Description       |
|-----------|-------------------------------|---------|-------------------|
| value     | ConditionalOnMissingClass[]   |         | Type condition array |

#### 4.3.7 `@ConditionalOnJava`
This annotation declares Java version range conditions.

| Parameter | Type      | Default | Description              |
|-----------|-----------|---------|--------------------------|
| value     | String    |         | Java version range, e.g. [1.8,) means version 1.8 or higher |

#### 4.3.8 `@Conditional`
This annotation represents custom annotation conditions.

| Parameter       | Type      | Default | Description                                                                                     |
|-----------------|-----------|---------|-------------------------------------------------------------------------------------------------|
| value           | String    |         | Implementation class for annotation matching. If empty, defaults to generating from annotation name.<br/>E.g. `ConditionalOnJava` annotation corresponds to `OnJavaCondition` in the same package. |
| dependOnLoader  | boolean   | false   | Indicates this condition depends on the application's class loader                              |

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConditionalOnClasses.class)
@Documented
@Conditional(dependOnLoader = true)
public @interface ConditionalOnClass {
    
    String value();

}
```

#### 4.3.8 `@ConditionalComposite`
This meta-annotation indicates the annotation is a composite annotation containing multiple basic conditions, used to simplify configuration.
```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(name = {
        GovernanceConfig.CONFIG_LIVE_ENABLED,
        GovernanceConfig.CONFIG_LANE_ENABLED
}, matchIfMissing = true, relation = ConditionalRelation.OR)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, value = "false")
@ConditionalComposite
public @interface ConditionalOnOnlyRouteEnabled {

}
```

### 4.4 `@Injectable`
该注解声明在扩展实现上，其配置参数如下

| 参数     | 类型      | 默认值  | 说明     |
|--------|---------|------|--------|
| value  | boolean | true | 是否开启注入 |

#### 4.4.1 `@Inject`
该注解声明在扩展实现的字段上，其配置参数如下

| 参数     | 类型      | 默认值   | 说明         |
|--------|---------|-------|------------|
| value  | String  | ""    | 组件名称       |
| nullable  | boolean | false | 值是否可以为null |

##### 4.4.1.1 注入系统组件

声明在字段上，则注入该名称的系统组件
```java
@Inject(Application.COMPONENT_APPLICATION)
private Application application;
```
常用的系统组件如下：

| 名称                                             | 类型      | 说明       |
|------------------------------------------------|---------|----------|
| AgentConfig.COMPONENT_AGENT_CONFIG             | AgentConfig  | 代理配置     |
| EnhanceConfig.COMPONENT_ENHANCE_CONFIG         | EnhanceConfig | 增强配置     |
| AgentPath.COMPONENT_AGENT_PATH,                | AgentPath | 代理路径     |
| Application.COMPONENT_APPLICATION              | Application | 应用实例     |
| Timer.COMPONENT_TIMER                          | Timer | 时钟轮      |
| ClassLoaderConfig.COMPONENT_CLASSLOADER_CONFIG | ClassLoaderConfig | 类加载器配置   |
| AgentLifecycle.COMPONENT_AGENT_LIFECYCLE       | AgentLifecycle | 代理声明周期管理 |
| Registry.COMPONENT_REGISTRY                    | Registry | 注册中心     |
| PolicySupervisor.COMPONENT_POLICY_SUPERVISOR   | PolicySupervisor | 策略管理器    |
| InvocationContext.COMPONENT_INVOCATION_CONTEXT | InvocationContext | 流控拦截上下文  |

##### 4.4.1.2 注入扩展实现

声明在扩展接口字段上，则注入该扩展的最高优先级的实现
```java
@Inject
private LoadBalancer loadBalancer;
```

声明在扩展接口的映射字段上，则注入所有扩展实现
```java
@Inject
private Map<String, LoadBalancer> loadBalancers;
```

##### 4.4.1.3 注入事件发布器
声明在事件发布器字段上，则注入该名称的事件发布器
```java
@Inject(Publisher.POLICY_SUBSCRIBER)
private Publisher<PolicySubscriber> policyPublisher;
```
