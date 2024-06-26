Lane Model
===

## 1. Lane Space

A tenant can have multiple lane spaces. The structure of a lane space is as follows:
```
.
└── Lane Space
    ├── Lanes(*)
    ├── Lane Rules(*)
    │   ├── Conditions(*)
    ├── Lane Domains(*)
    │   ├── Paths(*)
```

## 2. Lane

| Property     | Name      | Description                              |
|--------------|-----------|------------------------------------------|
| code         | Lane Code | Unique within the lane space             |
| name         | Name      |                                          |
| defaultLane  | Default Lane | `true`: Default lane<br> `false`: Non-default lane |

> Default Lane: When a microservice application is not deployed within a specified lane and no routing strategy for that lane is configured, it routes to the default lane instance.

```json
{
  "code": "production",
  "name": "production lane",
  "defaultLane": true
}
```

## 3. Lane Rules

Used by the gateway to determine coloring conditions.

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

## 4. Lane Domains

Used by the gateway to determine coloring based on domain names and paths. Paths are associated with lane rules. This simplifies management and optimizes gateway coloring performance.

Paths start with `/` and are matched as prefixes based on the delimiter. The longest matching path is selected.

For example, the request path `/mall/order/addOrder` matches the rules as follows:

1. `/mall/order/addOrder` matches, this rule is selected
2. `/mall/order` matches
3. `/mall` matches
4. `/mall/order/add` does not match
5. `/mall/or/addOrder` does not match

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

## 5. Model Skeleton

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

## 6. Microservice Lane Strategy

Please refer to the service lane strategy in the [Service Governance Model](./governance.md).