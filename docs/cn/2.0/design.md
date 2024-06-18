# 版本1遗留的的问题

1. 不能动态注入方式进行治理
2. 不支持J2EE多个应用
3. 不支持热更新治理扩展和插件
4. 不支持熔断策略

## 1. 动态增强设计

满足动态注入方式进行治理，支持多个应用，支持热更新。

## 1.1 类加载器设计

![pic](./image/classloader.png)

类加载器关系

```mermaid
classDiagram
direction BT
class LiveAgentClassLoader
class LiveAppClassLoader
class LivePluginClassLoader

LiveAgentClassLoader ..> SystemClassLoader
LiveAppClassLoader  ..>  LiveAgentClassLoader
LiveAppClassLoader  ..>  ApplicationClassLoader
LivePluginClassLoader  ..>  LiveAppClassLoader

```

为每个版本生成对于的多活应用加载器，方便热更新

## 1.2 目录结构

### 1.2.1 打包程序目录

把治理相关的API、扩展、插件和配置放到govern目录，便于按照应用来加载和动态更新

![pic](./image/package.png)

### 1.2.2 工程目录

```
.
└── joylive-agent
    ├── joylive-bom
    ├── joylive-bootstrap
    ├── joylive-core
    │   ├── joylive-core-api
    │   ├── joylive-core-bootstrap
    │   ├── joylive-core-extension-jplug
    │   ├── joylive-core-parser-jackson
    │   ├── ......
    ├── joylive-govern
    │   ├── joylive-governace-api
    │   ├── joylive-governace-metric-opentelemetry
    │   ├── joylive-governace-function-bkdrhash
    │   ├── joylive-governace-service-file
    │   ├── ......
    ├── joylive-plugin
    ├── joylive-demo
    ├── joylive-test
    ├── joylive-package

```
拆分了joylive-core和joylive-implement

## 1.2 配置

![pic](./image/config.png)

## 1.3 应用状态

```mermaid
classDiagram
    direction BT
    class StartStatus{
        <<enumeration>>
        STARTING
        STARTED
        READY
        STOP
        CLOSE
    }
    class GovernStatus{
        <<enumeration>>
        INITIAL
        SERVICE_READY
        ENHANCE_READY
        POLICY_READY
        REGISTRY_READY
        GOVERN_READY
        FAILED
    }
    
    class Application{
        - startStatus: StartStatus
        - governStatus: GovernStatus
    }
    
    Application --> StartStatus
    Application --> GovernStatus

```

| 状态       | 名称  | 入流量 | 出流量 | 说明                   |
|----------|-----|-----|-----|----------------------|
| STARTING | 启动中 | 否   | 是   |                      |
| PREPARED | 上下文就绪 | 否   | 是   |                      |
| STARTED  | 实例就绪 | 否   | 是   |                      |
| READY    | 启动就绪 | 是    | 是    | 流量治理插件需要根据当前状态来校验入流量 |
| STOPPED  | 停止  | 否    | 是    |                      |
| CLOSED   | 销毁关闭 | 否    | 是    |                      |
| FAILED   | 失败  | 否    | 是    |                      |

| 状态             | 名称       | 开启治理 | 说明                      |
|----------------|----------|------|-------------------------|
| INITIAL        | 初始状态     | 否    |                         |
| SERVICE_READY  | 同步相关服务就绪 | 否    |                         |
| ENHANCE_READY  | 增强植入就绪   | 否    |                         |
| POLICY_READY   | 服务策略就绪   | 否    |                         |
| REGISTRY_READY | 注册就绪     | 否    | 流量治理插件需要根据当前状态来判断是否跳过治理 |
| GOVERN_READY   | 治理就绪     | 是    |                         |
| FAILED         | 失败       | 否    |                         |

### 1.3.1 静态注入

1. 启动状态机

以Springboot来讲解状态机变化

```mermaid
stateDiagram
    direction TB
    [*] --> STARTING: start
    STARTING --> PREPARED: prepare context success
    STARTING --> FAILED: prepare context error
    PREPARED --> STARTED: refresh context success
    PREPARED --> FAILED: refresh context error
    STARTED --> READY: run success
    STARTED --> FAILED: run error
    READY --> STOPPED: stop
    READY --> CLOSED: close
    STOPPED --> STARTED: start
    FAILED --> CLOSED
    CLOSED --> [*]
    
```
2. 治理状态机

```mermaid
stateDiagram
    direction TB
    [*] --> INITIAL: start
    INITIAL --> SERVICE_READY: start service success
    INITIAL --> FAILED: start service error
    SERVICE_READY --> ENHANCE_READY: enhance success
    SERVICE_READY --> FAILED: enhance error
    ENHANCE_READY --> POLICY_READY: sync policy success
    ENHANCE_READY --> FAILED: sync policy error
    POLICY_READY --> REGISTRY_READY: registry success
    POLICY_READY --> FAILED: registry error
    REGISTRY_READY --> GOVERN_READY
    GOVERN_READY --> CLOSED:close
    FAILED --> CLOSED:close
    CLOSED --> [*]
    
```

### 1.3.2 动态注入

1. 启动状态机

初始状态为READY，应用可以继续接收流量

2. 治理状态机

初始状态为INITIAL

## 1.4 应用启动

静态注入Fatjar的时候，需要获取到应用类加载器

动态注入的时候，需要获取到应用上下文，并从中获取到应用提供的服务，便于同步服务的治理策略。同时需要刷新注册中心，注入多活的标签。

通过插件及应用提供者扩展，来感知应用的启动事件。多种方式可能触发了相同的应用启动事件，需要过滤掉重复的事件。

### 1.4.1 静态注入

#### 1.4.1.1 `Springboot`类加载器

Springboot支持Jar和War两种启动方式

静态注入可以拦截`org.springframework.boot.loader.LaunchedURLClassLoader`的构造函数，获取到应用类加载器并发出应用启动事件。

#### 1.4.1.2 `Springboot`事件拦截

静态注入可以拦截`org.springframework.boot.SpringApplicationRunListeners`的相关方法，触发应用事件

```mermaid
classDiagram
    direction BT
    class SpringApplicationRunListeners {
      ~ environmentPrepared(ConfigurableBootstrapContext, ConfigurableEnvironment) void
      ~ contextPrepared(ConfigurableApplicationContext) void
      ~ ready(ConfigurableApplicationContext, Duration) void
      ~ contextLoaded(ConfigurableApplicationContext) void
      ~ starting(ConfigurableBootstrapContext, Class~?~) void
      ~ started(ConfigurableApplicationContext, Duration) void
    }

```

### 1.4.2 动态注入

#### 1.4.2.1 动态接口

```mermaid
classDiagram
    direction BT
    class AppContext {
        <<Interface>>
        + getId() String
        + getName() String
        + getClassLoader() Classloader
        + ~T~ getBeans(type: Class~T~) List~T~
    }
    class AppContextProvider {
        <<Extensible>>
        + getContexts(): List~AppContext~
    }
    class JmxSpringContextProvider {
        <<Extension>>
        - queryName: String = "org.springframework.boot:type=Admin,name=*"
        - getApplicationContexts() List~~ConfigurableApplicationContext~
    }

    class SpringAppContext {
        - context: ConfigurableApplicationContext
    }
    
    class JmxUtils{
        + static ~T~ getMBeans(queryName:String, converter:Function~Object, T~) List~T~
    }

    AppContextProvider --> AppContext
    SpringAppContext ..|> AppContext
    JmxSpringContextProvider ..|> AppContextProvider
    JmxSpringContextProvider --> SpringAppContext
    JmxSpringContextProvider --> JmxUtils
```

#### 1.4.2.2 `JMX`获取`ConfigurableApplicationContext`

动态注入，需要拿到启动的应用上下文。

通过调用本地JMX方法获取MXBean。

1. 从JMX中查询MBean，并调用转换器进行转换
```java
@SuppressWarnings("unchecked")
public static <T> List<T> getMBeans(String queryName, Function<Object, T> converter) {
    List<T> result = new ArrayList<>();
    if (queryName != null && !queryName.isEmpty()) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Class<?> dynamicMBean2Class = Class.forName("com.sun.jmx.mbeanserver.DynamicMBean2");
            Method getResourceMethod = dynamicMBean2Class.getDeclaredMethod("getResource");
            Class<?> nameamedObjectClass = Class.forName("com.sun.jmx.mbeanserver.NamedObject");
            Method getObjectMethod = nameamedObjectClass.getDeclaredMethod("getObject");

            Field msbInterceptorField = server.getClass().getDeclaredField("mbsInterceptor");
            msbInterceptorField.setAccessible(true);
            Object mbsInterceptor = msbInterceptorField.get(server);

            Field repositoryField = mbsInterceptor.getClass().getDeclaredField("repository");
            repositoryField.setAccessible(true);
            Object repository = repositoryField.get(mbsInterceptor);

            Field domainTbField = repository.getClass().getDeclaredField("domainTb");
            domainTbField.setAccessible(true);
            Map<String, Map<String, ?>> domainTb = (Map<String, Map<String, ?>>) domainTbField.get(repository);

            Set<ObjectInstance> instances = server.queryMBeans(new ObjectName(queryName), null);
            for (ObjectInstance instance : instances) {
                ObjectName objectName = instance.getObjectName();
                Object namedObject = domainTb.get(objectName.getDomain()).get(objectName.getCanonicalKeyPropertyListString());
                Object dynamicMBean = getObjectMethod.invoke(namedObject);
                Object mbean = getResourceMethod.invoke(dynamicMBean);
                T target = converter != null ? converter.apply(mbean) : (T) mbean;
                if (target != null) {
                    result.add(target);
                }
            }
            return result;
        } catch (Throwable ignore) {
        }
    }
    return result;
}
```
2. 查询Springboot的MBean，并通过反射获取其上下文
```java
public List<ConfigurableApplicationContext> getApplicationContexts() {
    try {
        Field contextField = SpringApplicationAdminMXBeanRegistrar.class.getDeclaredField("applicationContext");
        contextField.setAccessible(true);
        return getMBeans(queryName, target -> {
            try {
                Field outerField = target.getClass().getDeclaredField("this$0");
                outerField.setAccessible(true);
                Object registrar = outerField.get(target);
                return (ConfigurableApplicationContext) contextField.get(registrar);
            } catch (Throwable e) {
                return null;
            }
        });
    } catch (Throwable e) {
        return new ArrayList<>();
    }
}
```
MBean的查询名称可以作为参数传递

### 1.5 服务策略订阅

在注册中心插件里面，拦截器的构造函数里面，通过当前应用的上下文对象拿到已经创建好的注册对象进行订阅

### 1.6 服务注册元数据更新

#### 1.6.1 Nacos

nacos支持在心跳事件里面更新元数据，可以拦截心跳事件，根据应用状态对元数据进行重新赋值。

#### 1.6.2 其它

在注册中心插件里面，拦截器的构造函数里面，注册应用事件，当服务策略就绪的时候，异步注销和重新注册服务。

