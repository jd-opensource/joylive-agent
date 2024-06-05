多活治理模型
===

应用多活通常包括同城多活和异地多活，异地多活可采用单元化技术来实现。

## 1.多活空间

一个组合可以有多个多活空间，多活空间构成如下所示：
```
.
└── 多活空间
    ├── 单元路由变量(*)
    ├── 单元(*)
    │   ├── 分区(*)
    ├── 单元规则(*)
    ├── 多活域名(*)
    │   ├── 单元子域名(*)
    │   ├── 路径(*)
    │   │   ├── 业务参数(*)

```
## 2.单元

单元是逻辑的，一般对应一个地域，单元化常用于异地多活场景。 
1. 单元化通常按用户维度来进行核心业务和数据拆分，每个单元内拥有自己的数据，尽量在单元内闭环调用；
2. 用户请求尽量就近路由到所属单元进行访问；
3. 当一个单元出现故障的时候，所影响的用户范围减少，其它单元还可以正常工作；
4. 单元间的数据可以双向同步，可以把故障单元的用户，一键调拨到到其它单元，大大减少了RTO。

| 属性         | 名称   | 说明                                                                                    |
|------------|------|---------------------------------------------------------------------------------------|
| code       | 单元代码 | 在企业内全局规划，唯一                                                                           |
| name       | 名称   |                                                                                       |
| type       | 类型   | `CENTER`：中心单元，除了承接区域性用户流量，还提供全局、强一致性和数据分析的服务，目前限定有且只有一个中心单元<br> `UNIT`：普通单元只能承接区域性的流量 |
| accessMode | 读写权限 | 用于容灾切换场景确保数据一致性<br/>`READ_WRITE`：可读可写<br/>`READ_ONLY`：只读<br/>`NONE`：无权限               |
| labels     | 标签   | 例如设置地域和可用区                                                                            |
| cells      | 分区   | 该单元下的逻辑分区                                                                             |

```json
{
  "code": "unit1",
  "name": "unit1",
  "type": "UNIT",
  "accessMode": "READ_WRITE",
  "labels": {
    "region": "region1"
  },
  "cells": [
  ]
}
```

## 2.1 单元分区

单元分区是逻辑的，一般对应一个云上的可用区或物理数据中心。

| 属性         | 名称   | 说明                                                                      |
|------------|------|-------------------------------------------------------------------------|
| code       | 代码   | 在企业内全局规划，唯一                                                             |
| name       | 名称   |                                                                         |
| accessMode | 读写权限 | 用于容灾切换场景确保数据一致性<br/>`READ_WRITE`：可读可写<br/>`READ_ONLY`：只读<br/>`NONE`：无权限 |
| labels     | 标签   | 例如设置地域和可用区                                                              |

```json
{
  "code": "cell1",
  "name": "cell1",
  "accessMode": "READ_WRITE",
  "labels": {
    "zone": "zone1"
  }
}
```

## 3.路由变量

单元路由变量是单元间进行流量路由的依据，通常指的用户账号。

| 属性      | 名称   | 说明         |
|---------|------|------------|
| name    | 名称   |            |
| type    | 类型   |            |
| sources | 取值方式 | 可以定义多个取值方式 |

```json
{
  "name": "user",
  "type": "unit",
  "sources": [
  ]
}
```

### 3.1 变量取值方式

路由变量可以包括多个取值方式，对应在不同业务的入口域名的获取方式。 例如可以定义从Cookie里面获取用户变量user。

变量可以定义转换函数，通过实现变量函数扩展来获取到真实的用户标识，例如通过会话ID从缓存中拿到用户ID

| 属性     | 名称     | 说明                                                |
|--------|--------|---------------------------------------------------|
| name   | 名称     |                                                   |
| scope  | HTTP值域 | `QUERY`：请求参数<br/>`HEADER`：请求头<br/>`COOKIE`：COOKIE |
| key    | 键      |                                                   |
| func   | 转换函数   | 扩展`VariableFunction`的实现名称                         |
| header | 存储键    | 变量转换获取后存储到HEADER，可用于入口网关层逻辑，便于后续读取。               |

```json
{
  "name": "getUserByQuery",
  "scope": "QUERY",
  "key": "user",
  "func": "",
  "header": ""
}
```

## 4.单元规则

单元规则用于定义单元之间和分区之间的流量调拨规则

同城多活表示只有一个单元，可以有多个分区

| 属性                    | 名称       | 说明                                             |
|-----------------------|----------|------------------------------------------------|
| id                    | 唯一编号     |                                                |
| name                  | 名称       |                                                |
| liveType              | 多活类型     | CROSS_REGION_LIVE：单元化<br/>ONE_REGION_LIVE：同城多活 |
| variable              | 单元路由变量   |                                                |
| variableSource        | 变量默认取值方式 |                                                |
| variableFunction      | 变量计算函数   | 用于计算出的值取模后来判断所属单元，扩展`UnitFunction`的实现名称        |
| variableMissingAction | 变量缺失的操作  | REJECT：拒绝<br/>CENTER：路由中心                      |
| modulo                | 模数       | 计算出的值取模后来判断所属单元                                |
| units                 | 单元路由规则   |                                                |

```json
{
  "id": "1003",
  "name": "Test",
  "liveType": "CROSS_REGION_LIVE",
  "business": "",
  "variable": "user",
  "variableSource": "getUserByQuery",
  "variableFunction": "BKDRHash",
  "variableMissingAction": "CENTER",
  "modulo": 10000,
  "units": [
  ]
}
```

### 4.1 单元路由规则

单元路由规则描述单元路由信息

| 属性       | 名称         | 说明                       |
|----------|------------|--------------------------|
| code     | 单元编码       |                          |
| allows   | 允许的路由变量白名单 | 相等判断                     |
| prefixes | 允许的路由变量前缀  | 前缀判断                     |
| ranges   | 值区间        | 数组，计算出的值取模后的半开区间，[起始,截止) |
| cells    | 分区         | 单元下的分区路由规则               |

```json
{
  "code": "unit1",
  "allows": [
  ],
  "prefixes": [
    "unit1"
  ],
  "ranges": [
    {
      "from": 0,
      "to": 8000
    }
  ],
  "cells": [
  ]
}
```

#### 4.1.1 分区路由规则

分区路由规则描述分区路由信息

| 属性       | 名称         | 说明      |
|----------|------------|---------|
| code     | 分区编码       |         |
| allows   | 允许的路由变量白名单 | 相等判断    |
| prefixes | 允许的路由变量前缀  | 前缀判断    |
| weight   | 权重         | 分区之间的权重 |

```json
{
  "code": "cell1",
  "allows": [
  ],
  "prefixes": [
  ],
  "weight": 40
}
```

## 5.多活域名

多活域名描述启用多活的域名，通常用于网关拦截入口流量按照匹配的单元规则进行单元路由。

| 属性                | 名称        | 说明                                             |
|-------------------|-----------|------------------------------------------------|
| host              | 域名        |                                                |
| protocols         | 支持的协议     |                                                |
| liveType          | 多活类型      | CROSS_REGION_LIVE：单元化<br/>ONE_REGION_LIVE：同城多活 |
| correctionType    | 纠错类型      | UPSTREAM：转发                                    |
| unitDomainEnabled | 是否启用单元子域名 | 如果启用了单元子域名，则进行HTTP转发的时候，则转发到单元子域名上             |
| unitDomains       | 单元子域名     | 单元子域名用于HTTP Web请求或回调闭环在单元                      |
| paths             | 路径规则      |                                                |

```json
{
  "host": "demo.live.local",
  "protocols": [
    "http",
    "https"
  ],
  "liveType": "CROSS_REGION_LIVE",
  "correctionType": "UPSTREAM",
  "unitDomainEnabled": true,
  "unitDomains": [
  ],
  "paths": [
  ],
  "resources": []
}
```

### 5.1 单元子域名

单元子域名描述主域名在各个单元的子域名

| 属性      | 名称     | 说明            |
|---------|--------|---------------|
| unit    | 单元     |               |
| host    | 子域名    | 默认是`单元编码-主域名` |
| backend | 后端转发地址 |               |

```json
{
  "unit": "unit1",
  "host": "unit1-demo.live.local",
  "backend": ":8080"
}
```

### 5.2 路径规则

路径规则描述路径上匹配的单元规则，以`/`开始，按照路径分隔进行前缀匹配。取最长匹配路径

例如请求路径`/mall/order/addOrder`匹配的规则如下：

1. `/mall/order/addOrder` 匹配，选择该条规则
2. `/mall/order` 匹配
3. `/mall` 匹配
4. `/mall/order/add` 不匹配
5. `/mall/or/addOrder` 不匹配

| 属性                   | 名称                            | 说明                                                |
|----------------------|-------------------------------|---------------------------------------------------|
| path                 | 路径                            | 以`/`开始，按照路径分隔进行前缀匹配                               |
| ruleId               | 单元规则ID                        |                                                   |
| customVariableSource | 是否自定义变量取值方式，如果没有开启则沿用规则上的默认定义 |                                                   |
| variable             | 变量                            | 开启自定义变量取值方式时候设置                                   |
| variableSource       | 变量来源                          | 开启自定义变量取值方式时候设置                                   |
| bizVariableEnabled   | 启用请求参数规则                      | 用于统一网关，用参数来区分重要业务                                 |
| bizVariableName      | 业务参数名称                        |                                                   |
| bizVariableScope     | 业务参数HTTP值域                    | `QUERY`：请求参数<br/>`HEADER`：请求头<br/>`COOKIE`：COOKIE |

```json
{
  "path": "/",
  "ruleId": "1003",
  "ruleName": "Test",
  "customVariableSource": false,
  "variable": "user",
  "variableSource": "getUserByQuery",
  "bizVariableEnabled": false,
  "bizVariableName": "",
  "bizVariableScope": null,
  "bizVariableRules": []
}
```

#### 5.2.1 业务参数规则

业务参数规则根据参数值来进行路由规则匹配

| 属性       | 名称     | 说明 |
|----------|--------|----|
| value    | 参数值    |    |
| ruleId   | 单元规则ID |    |
| ruleName | 单元规则名称 |    |

```json
{
  "value": "order",
  "ruleId": "1003",
  "ruleName": "Test"
}
```

## 6. 模型骨架

```json
[
  {
    "apiVersion": "apaas.cos.com/v2alpha1",
    "kind": "MultiLiveSpace",
    "metadata": {
      "name": "mls-abcdefg1",
      "namespace": "apaas-livespace"
    },
    "spec": {
      "id": "v4bEh4kd6Jvu5QBX09qYq-qlbcs",
      "code": "7Jei1Q5nlDbx0dRB4ZKd",
      "name": "TestLiveSpace",
      "version": "2023120609580935201",
      "tenantId": "tenant1",
      "units": [
      ],
      "domains": [
      ],
      "unitRules": [
      ],
      "variables": [
      ]
    }
  }
]
```



