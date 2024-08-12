Extension System
===

Based on the Java SPI mechanism, it provides annotation, dependency injection, and configuration functionalities.

## 1. Define Extension

Define an extension interface and use the `@Extensible` annotation to declare the extension.

The following defines an extension interface for `AgentService`:

```java
@Extensible("AgentService")
public interface AgentService {
    
    CompletableFuture<Void> start();
    
    CompletableFuture<Void> stop();
    
}
```

## 2. Extension Implementation

Implement the extension interface and use the `@Extension` annotation to declare the extension implementation.

The following implements a file synchronization service `LiveSpaceFileSyncer`:

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
Annotations on this class:
1. The `@Extension` annotation declares the extension implementation and provides a name.
2. The `@ConditionalOnProperty` annotation declares the conditions for enabling, allowing multiple conditions to be combined.
3. The `@Injectable` annotation declares that the type requires automatic injection.
4. The `@Inject` annotation declares that the field requires automatic injection.
5. The `@Config` annotation declares that the field requires automatic configuration.

## 3. Enable Extension
1. Configure the full path name of the extension in the SPI file `META-INF/services/com.jd.live.agent.core.service.AgentService`:
   `com.jd.live.agent.implement.service.policy.file.LiveSpaceFileSyncer`
2. Configure the live space synchronization type as `file`.

## 4. Annotation Description
### 4.1 `@Extensible`

This annotation is declared on the extension interface with the following configuration parameters:

| Parameter       | Type     | Default Value | Description      |
|-----------------|----------|---------------|------------------|
| value           | String   |               | Extension interface name |

### 4.2 `@Extension`
This annotation is declared on the extension implementation with the following configuration parameters:

| Parameter | Type    | Default Value   | Description                |
|-----------|---------|-----------------|----------------------------|
| value     | String  |                 | Extension implementation name |
| order     | int     | Short.MAX_VALUE | Priority, the smaller the value, the higher the priority |
| singleton | boolean | true            | Whether it is a singleton   |

### 4.3 Enabling Conditions
Conditions can be configured on the extension implementation.

#### 4.3.1 `@ConditionalOnProperty`
This annotation declares the configuration item switch for enabling. Multiple `@ConditionalOnProperty` annotations can be configured, with an `AND` relationship between each.

| Parameter       | Type                  | Default Value | Description                        |
|-----------------|-----------------------|---------------|------------------------------------|
| value           | String                |               | Configuration value                |
| name            | String[]              |               | Configuration name array           |
| matchIfMissing  | boolean               | true          | Whether to match if not configured |
| relation        | ConditionalRelation   | OR            | Relation<br/>OR or<br/>AND         |
| caseSensitive   | boolean               | false         | Case sensitivity                   |

#### 4.3.2 `@ConditionalOnProperties`
This annotation declares multiple configuration item switches, consisting of multiple `@ConditionalOnProperty` annotations, with a relationship between each.

| Parameter       | Type                      | Default Value | Description                        |
|-----------------|---------------------------|---------------|------------------------------------|
| value           | ConditionalOnProperty[]   |               | Configuration item array           |
| relation        | ConditionalRelation       | AND           | Relation<br/>OR or<br/>AND         |

#### 4.3.3 `@ConditionalOnClass`
This annotation declares the type condition for enabling. If the type exists, it matches. Multiple `@ConditionalOnClass` annotations can be configured, with an `AND` relationship between each.

| Parameter | Type    | Default Value | Description   |
|-----------|---------|---------------|---------------|
| value     | String  |               | Full path class name |

#### 4.3.4 `@ConditionalOnMissingClass`
This annotation declares the type condition for enabling. If the type does not exist, it matches. Multiple `@ConditionalOnMissingClass` annotations can be configured, with an `AND` relationship between each.

| Parameter | Type    | Default Value | Description   |
|-----------|---------|---------------|---------------|
| value     | String  |               | Full path class name |

### 4.4 `@Injectable`
This annotation is declared on the extension implementation with the following configuration parameters:

| Parameter | Type    | Default Value | Description         |
|-----------|---------|---------------|---------------------|
| value     | boolean | true          | Whether to enable injection |

#### 4.4.1 `@Inject`
This annotation is declared on fields of the extension implementation with the following configuration parameters:

| Parameter | Type    | Default Value | Description             |
|-----------|---------|---------------|-------------------------|
| value     | String  | ""            | Component name          |
| nullable  | boolean | false         | Whether the value can be null |

##### 4.4.1.1 Injecting System Components

Declare on a field to inject the system component with the specified name:

```java
@Inject(Application.COMPONENT_APPLICATION)
private Application application;
```
Common system components include:

| Name                                             | Type               | Description              |
|--------------------------------------------------|--------------------|--------------------------|
| AgentConfig.COMPONENT_AGENT_CONFIG               | AgentConfig        | Agent configuration      |
| EnhanceConfig.COMPONENT_ENHANCE_CONFIG           | EnhanceConfig      | Enhancement configuration |
| AgentPath.COMPONENT_AGENT_PATH                   | AgentPath          | Agent path               |
| Application.COMPONENT_APPLICATION                | Application        | Application instance     |
| Timer.COMPONENT_TIMER                            | Timer              | Timer wheel              |
| ClassLoaderConfig.COMPONENT_CLASSLOADER_CONFIG   | ClassLoaderConfig  | Class loader configuration |
| AgentLifecycle.COMPONENT_AGENT_LIFECYCLE         | AgentLifecycle     | Agent lifecycle management |
| ConditionMatcher.COMPONENT_CONDITION_MATCHER     | ConditionMatcher   | Condition matcher        |
| PolicySupervisor.COMPONENT_POLICY_SUPERVISOR     | PolicySupervisor   | Policy supervisor        |
| InvocationContext.COMPONENT_INVOCATION_CONTEXT   | InvocationContext  | Flow control intercept context |

##### 4.4.1.2 Injecting Extension Implementations

Declare on an extension interface field to inject the highest priority implementation of that extension:

```java
@Inject
private LoadBalancer loadBalancer;
```

Declare on a map field of the extension interface to inject all extension implementations:

```java
@Inject
private Map<String, LoadBalancer> loadBalancers;
```

##### 4.4.1.3 Injecting Event Publishers
Declare on an event publisher field to inject the event publisher with the specified name:

```java
@Inject(Publisher.POLICY_SUBSCRIBER)
private Publisher<PolicySubscriber> policyPublisher;
```