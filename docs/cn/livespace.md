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

单元分为中心单元和普通单元，其中：
1. 中心单元，除了承接区域性用户流量，还提供全局、强一致性和数据分析的服务，目前限定有且只有一个中心单元。
2. 普通单元只能承接区域性的流量。

## 2.1 分区

分区是逻辑的，一般对应一个云上的可用区或物理数据中心。
1. 单元化通常按用户维度来进行核心业务和数据拆分，每个单元内拥有自己的数据，尽量在单元内闭环调用；
2. 用户请求尽量就近路由到所属单元进行访问；
3. 当一个单元出现故障的时候，所影响的用户范围减少，其它单元还可以正常工作；
4. 单元间的数据可以双向同步，可以把故障单元的用户，一键调拨到其它单元，大大减少了RTO。

单元分为中心单元和普通单元，其中：
1. 中心单元，除了承接区域性用户流量，还提供全局、强一致性和数据分析等共享服务，目前限定有且只有一个中心单元。
2. 普通单元只能承接区域性的流量。

## 3.单元路由变量

单元路由变量是单元间进行流量路由的依据，通常指的用户账号。

路由变量可以包括多个取值方式，对应在不同业务的入口域名的获取方式。 例如可以定义从Cookie里面获取用户变量user。

变量可以定义转换函数，通过实现变量函数扩展来获取到真实的用户标识，例如通过会话ID从缓存中拿到用户ID

## 4.单元规则

单元规则用于定义单元之间和分区之间的流量调拨规则

同城多活表示只有一个单元，可以有多个分区

| 属性 | 名称         | 说明                                           |
|----|------------|----------------------------------------------|
| liveType   | 多活类型       | 单元化(CROSS_REGION_LIVE)，同城多活(ONE_REGION_LIVE) |
| variable   | 路由变量       |                                              |
| variableSource   | 变量默认取值方式   |                                              |
| variableFunction   | 变量计算Hash函数 | 计算出的值取模后来判断所属单元                              |
| variableMissingAction   | 变量缺失的操作    | 拒绝(REJECT)，路由中心(CENTER)                      |
| modulo   | 模数         | 计算出的值取模后来判断所属单元                              |
| units   | 单元路由规则     |                                              |

### 4.1 单元路由规则

单元路由规则描述单元路由信息

| 属性       | 名称         | 说明                       |
|----------|------------|--------------------------|
| code     | 单元编码       |                          |
| allows   | 允许的路由变量白名单 | 数组                       |
| prefixes | 允许的路由变量前缀  | 数组                       |
| ranges   | 值区间        | 数组，计算出的值取模后的半开区间，[起始,截止) |
| cells    | 分区         | 单元下的分区路由规则               |

#### 4.1.1 分区路由规则

分区路由规则描述分区路由信息

| 属性       | 名称         | 说明      |
|----------|------------|---------|
| code     | 分区编码       |         |
| allows   | 允许的路由变量白名单 | 数组      |
| prefixes | 允许的路由变量前缀  | 数组      |
| weight   | 权重         | 分区之间的权重 |

## 5.多活域名

多活域名描述启用多活的域名，通常用于网关拦截入口流量按照匹配的单元规则进行单元路由。

| 属性       | 名称        | 说明                                           |
|----------|-----------|----------------------------------------------|
| host     | 域名        |                                              |
| liveType   | 多活类型      | 单元化(CROSS_REGION_LIVE)，同城多活(ONE_REGION_LIVE) |
| correctionType | 纠错类型      | 转发(UPSTREAM)                                 |
| unitDomainEnabled   | 是否启用单元子域名 |                                              |
| unitDomains   | 单元子域名     | 单元子域名用于HTTP Web请求或回调闭环在单元                    |
| paths   | 路径规则      |                                              |

### 5.1 单元子域名

单元子域名描述主域名在各个单元的子域名

| 属性       | 名称        | 说明                        |
|----------|-----------|---------------------------|
| unit     | 单元        |                           |
| host   | 子域名       | 默认是`单元编码-主域名`             |

### 5.2 路径规则

路径规则描述路径上匹配的单元规则

| 属性     | 名称          | 说明                  |
|--------|-------------|---------------------|
| path   | 路径          | 以`/`开始，按照路径分隔进行前缀匹配 |
| ruleId | 单元规则ID      |                     |
| customVariableSource | 是否自定义变量取值方式 |          |
| variable | 变量          |                     |
| variableSource | 变量来源        |                     |

## 6. 完整样例

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
        {
          "code": "unit1",
          "name": "unit1",
          "type": "UNIT",
          "accessMode": "READ_WRITE",
          "labels": {
            "region": "region1"
          },
          "cells": [
            {
              "code": "cell1",
              "name": "cell1",
              "accessMode": "READ_WRITE",
              "labels": {
                "zone": "zone1"
              }
            },
            {
              "code": "cell2",
              "name": "cell2",
              "accessMode": "READ_WRITE",
              "labels": {
                "zone": "zone2"
              }
            },
            {
              "code": "cell3",
              "name": "cell3",
              "accessMode": "READ_WRITE",
              "labels": {
                "zone": "zone3"
              }
            }
          ]
        },
        {
          "code": "unit2",
          "name": "unit2",
          "type": "UNIT",
          "accessMode": "READ_WRITE",
          "labels": {
            "region": "region2"
          },
          "cells": [
            {
              "code": "cell4",
              "name": "cell4",
              "accessMode": "READ_WRITE",
              "labels": {
                "zone": "zone4"
              }
            }
          ]
        }
      ],
      "domains": [
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
            {
              "unit": "unit1",
              "host": "unit1-demo.live.local",
              "backend": ":8080"
            },
            {
              "unit": "unit2",
              "host": "unit2-demo.live.local",
              "backend": ":8080"
            }
          ],
          "paths": [
            {
              "path": "/",
              "ruleId": 1003,
              "ruleName": "Test",
              "customVariableSource": false,
              "variable": "user",
              "variableSource": "getUserByQuery",
              "bizVariableEnabled": false,
              "bizVariableName": "",
              "bizVariableScope": null,
              "bizVariableRules": []
            }
          ],
          "resources": []
        }
      ],
      "unitRules": [
        {
          "id": 1003,
          "name": "Test",
          "liveType": "CROSS_REGION_LIVE",
          "business": "",
          "variable": "user",
          "variableSource": "getUserByQuery",
          "variableFunction": "BKDRHash",
          "variableMissingAction": "CENTER",
          "modulo": 10000,
          "units": [
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
                {
                  "code": "cell1",
                  "allows": [],
                  "prefixes": [],
                  "weight": 40
                },
                {
                  "code": "cell2",
                  "allows": [],
                  "prefixes": [],
                  "weight": 40
                },
                {
                  "code": "cell3",
                  "allows": [],
                  "prefixes": [],
                  "weight": 20
                }
              ]
            },
            {
              "code": "unit2",
              "allows": [
              ],
              "prefix": [
                "unit2"
              ],
              "ranges": [
                {
                  "from": 8000,
                  "to": 10000
                }
              ],
              "cells": [
                {
                  "code": "cell4",
                  "allows": [],
                  "prefixes": [],
                  "weight": 100
                }
              ]
            }
          ]
        }
      ],
      "variables": [
        {
          "name": "user",
          "type": "unit",
          "sources": [
            {
              "name": "getUserByQuery",
              "scope": "QUERY",
              "key": "user",
              "func": "",
              "header": ""
            }
          ]
        }
      ]
    }
  }
]
```



