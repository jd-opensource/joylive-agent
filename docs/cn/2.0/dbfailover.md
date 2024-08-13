# 数据库切换

## 数据库多活策略

在多活空间中导入多活数据库，配置其关系和数据同步

```mermaid
classDiagram
    direction BT

    class DatabaseCluster {
        - uid: String
        - name: String
        - host: String
        - port: int
        - unit: String
        - cell: String
    }

    class ClusterRole {
        <<enumeration>>
        PRIMARY
        STANDBY
    }

    class ClusterRelation {
        - uid: String
        - sourceId: String
        - sourceRole: ClusterRole
        - targetId: String
        - targetRole: ClusterRole
    }

    class DatabaseSpace {
        - uid: String
        - clusters: List~DatabaseCluster~
        - relations: List~ClusterRelation~
        + getCluster(host: String, port: int) DatabaseCluster
        + getRelation(sourceId: String) List~ClusterRelation~
    }

    class GovernancePolicy {
        - databases: List~DatabaseSpace~
    }

    ClusterRelation ..> ClusterRole
    DatabaseSpace o-- DatabaseCluster
    DatabaseSpace o-- ClusterRelation
    GovernancePolicy o-- DatabaseSpace

```

## 多活数据源

多活数据源
```mermaid
classDiagram
    direction BT

    class LiveDataSource~T~ {
        <<abstract>>
        - delegate: T
        - host: String
        - port: int
        - failover: Boolean
        - policySupplier: PolicySupplier
        + failover(host: String, port: int) void
    }
    class LiveConnection {
        - delegate: Connection
    }
    class LiveStatement {
        - delegate: Statement
    }
    class LivePreparedStatement {
        - delegate: PreparedStatement
    }
    class LiveCallableStatement {
        - delegate: CallableStatement
    }

    class LiveDataSourceFactory {
        <<interface>>
        create(dataSource: DataSource) LiveDataSource
    }

    LiveDataSource ..|> DataSource
    LiveConnection ..|> Connection
    LiveStatement ..|> Statement
    LivePreparedStatement ..|> PreparedStatement
    LiveCallableStatement ..|> CallableStatement
    LiveDataSource ..> LiveConnection
    LiveConnection ..> LiveStatement
    LiveConnection ..> LivePreparedStatement
    LiveConnection ..> LiveCallableStatement
    LiveDataSourceFactory ..> LiveDataSource

```
### Druid

```mermaid
classDiagram
    direction BT

    class LiveDruidFactory {
    }
    
    class LiveDruidDataSource~DruidDataSource~ {
    }

    LiveDruidFactory ..|> LiveDataSourceFactory
    LiveDruidDataSource --|> LiveDataSource
    LiveDruidFactory ..> LiveDruidDataSource
    
```

### C3P0

```mermaid
classDiagram
    direction BT

    class LiveC3P0Factory {
    }

    class LiveC3P0DataSource~ComboPooledDataSource~ {
    }

    LiveC3P0Factory ..|> LiveDataSourceFactory

    LiveC3P0DataSource --|> LiveDataSource
    
    LiveC3P0Factory ..> LiveC3P0DataSource
    
```

### Hikari

```mermaid
classDiagram
    direction BT

    class LiveHikariFactory {
    }
    
    class LiveHikariDataSource~HikariDataSource~ {
    }

    LiveHikariFactory ..|> LiveDataSourceFactory

    LiveHikariDataSource --|> LiveDataSource

    LiveHikariFactory ..> LiveHikariDataSource
    
```

### DBCP

```mermaid
classDiagram
    direction BT

    class LiveDBCPFactory {
    }

    class LiveDBCPDataSource~BasicDataSource~ {
    }

    LiveDBCPFactory ..|> LiveDataSourceFactory

    LiveDBCPDataSource --|> LiveDataSource

    LiveDBCPFactory ..> LiveDBCPDataSource
    
```

## 数据库插件

拦截连接池，获取数据库，订阅数据库策略

### SpringBoot

DataSourceBuilder

```mermaid
classDiagram
    direction BT
    class DataSourceBuilderDefinition {
    }
    class DataSourceBuilderInterceptor {
    }
    
    DataSourceBuilderDefinition ..> DataSourceBuilderInterceptor
    DataSourceBuilderInterceptor ..|> InterceptorAdaptor
    DataSourceBuilderDefinition --|> PluginDefinitionAdapter
    DataSourceBuilderInterceptor ..> LiveDataSource

```
DataSourceBuilderInterceptor拦截build方法，订阅数据库策略，构造LiveDataSource返回
在Statement中进行禁读禁写的判断

## 数据库切换

### 切换开关

| 级别   | 配置项                                       | 说明                     |
|------|-------------------------------------------|------------------------|
| 集群级别 | live.failover                             | 在数据源URL上配置             |
| 应用级别 | agent.governance.database.defaultFailover | 在代理配置文件中设置，默认切换标识      |
| 应用级别 | agent.switch.database.enabled             | 在代理配置文件中设置，是否开启数据库切换增强 |

### 切换触发
1. 多活管控面切换相关数据库的关系及同步
2. 代理同步最新的数据库策略，触发数据库集群切换事件
3. 监听器订阅数据库集群切换事件，切换数据库集群