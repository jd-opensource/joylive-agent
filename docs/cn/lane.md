泳道模型
===

## 1. 泳道空间

一个租户可以有多个泳道空间，泳道空间构成如下所示：
```
.
└── 泳道空间
    ├── 泳道(*)
    ├── 泳道规则(*)
    │   ├── 条件(*)
    ├── 泳道域名(*)
    │   ├── 路径(*)

```

## 2. 泳道

| 属性         | 名称   | 说明                            |
|------------|------|-------------------------------|
| code       | 泳道代码 | 在泳道空间内唯一                      |
| name       | 名称   |                               |
| defaultLane | 默认泳道 | `true`：默认泳道<br> `false`：非默认泳道 |

> 默认泳道：当一个微服务应用在指定泳道内没有部署的时候，并且没有配置该泳道的路由策略，则路由到默认泳道实例。

```json
{
  "code": "production",
  "name": "production lane",
  "defaultLane": true
}
```

## 3. 泳道规则

用于网关进行染色条件判断

```json
{
  "id": 1,
  "conditions": {
    "beta": {
      "conditions": [
        {
          "type": "query",
          "opType": "EQUAL",
          "key": "beta",
          "values": [
            "true"
          ]
        }
      ]
    }
  }
}
```

## 4. 泳道域名

用于网关进行染色的域名和路径判断，路径上和泳道规则进行关联。简化管理，优化网关染色性能。

路径以`/`开始，按照分隔符进行前缀匹配。取最长匹配路径

例如请求路径`/mall/order/addOrder`匹配的规则如下：

1. `/mall/order/addOrder` 匹配，选择该条规则
2. `/mall/order` 匹配
3. `/mall` 匹配
4. `/mall/order/add` 不匹配
5. `/mall/or/addOrder` 不匹配

```json
{
  "host": "demo.live.local",
  "paths": [
    {
      "path": "/",
      "ruleId": 1
    }
  ]
}
```

## 5. 模型骨架

```json
[
  {
    "id": "1",
    "lanes": [
    ],
    "rules": [
    ],
    "domains": [
    ]
  }
]
```

## 6. 微服务泳道策略

请参阅[服务治理模型](./governance.md)中的服务泳道策略。


