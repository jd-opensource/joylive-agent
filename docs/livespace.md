Multi-Active Governance Model
===

Application of multi-active usually includes intra-city multi-active and inter-city multi-active, with inter-city multi-active implemented using unit technology.

## 1. Multi-Active Space

A rent can have multiple multi-active spaces, structured as follows:
```
.
└── Multi-Active Space
    ├── Unit Routing Variable(*)
    ├── Unit(*)
    │   ├── Cell(*)
    ├── Unit Rule(*)
    ├── Live Domain(*)
    │   ├── Unit Subdomain(*)
    │   ├── Path(*)
    │   │   ├── Business Parameter(*)

```
## 2. Unit

A unit is logical, generally corresponding to a region, and cell is often used in inter-city multi-active scenarios.
1. Unit typically splits core business and data by user dimension, with each unit having its own data, aiming for closed-loop calls within the unit;
2. User requests are routed to their respective units as close as possible;
3. When a unit fails, the affected user range is reduced, and other units can continue to operate normally;
4. Data between units can be synchronized bidirectionally, allowing users from the failed unit to be reallocated to other units with a single click, significantly reducing RTO.

| Attribute    | Name       | Description                                                                                  |
|--------------|------------|----------------------------------------------------------------------------------------------|
| code         | Unit Code  | Globally planned within the enterprise, unique                                                |
| name         | Name       |                                                                                              |
| type         | Type       | `CENTER`: Central unit, besides handling regional user traffic, also provides global, strong consistency, and data analysis services. Currently, there is only one central unit.<br> `UNIT`: Regular unit can only handle regional traffic |
| accessMode   | Access Mode| Used in disaster recovery scenarios to ensure data consistency<br/>`READ_WRITE`: Read and write<br/>`READ_ONLY`: Read only<br/>`NONE`: No access |
| labels       | Labels     | For example, set region and availability zone                                                 |
| cells        | Partitions | Logical partitions under this unit                                                            |

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

## 2.1 Cell

A cell is logical, generally corresponding to an availability zone in the cloud or a physical data center.

| Attribute    | Name       | Description                                                                                  |
|--------------|------------|----------------------------------------------------------------------------------------------|
| code         | Code       | Globally planned within the enterprise, unique                                                |
| name         | Name       |                                                                                              |
| accessMode   | Access Mode| Used in disaster recovery scenarios to ensure data consistency<br/>`READ_WRITE`: Read and write<br/>`READ_ONLY`: Read only<br/>`NONE`: No access |
| labels       | Labels     | For example, set region and availability zone                                                 |

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

## 3. Routing Variable

Unit routing variables are the basis for traffic routing between units, usually referring to user accounts.

| Attribute    | Name       | Description                                                                                  |
|--------------|------------|----------------------------------------------------------------------------------------------|
| name         | Name       |                                                                                              |
| type         | Type       |                                                                                              |
| sources      | Sources    | Multiple value sources can be defined                                                        |

```json
{
  "name": "user",
  "type": "unit",
  "sources": [
  ]
}
```

### 3.1 Variable Value Source

Routing variables can include multiple value sources, corresponding to different methods of obtaining values at business entry domains. For example, you can define a method to get the user variable from a Cookie.

Variables can define transformation functions to obtain the real user identifier by implementing variable function extensions, such as obtaining the user ID from the cache via session ID.

| Attribute    | Name       | Description                                                                                  |
|--------------|------------|----------------------------------------------------------------------------------------------|
| name         | Name       |                                                                                              |
| scope        | HTTP Scope | `QUERY`: Request parameter<br/>`HEADER`: Request header<br/>`COOKIE`: COOKIE                 |
| key          | Key        |                                                                                              |
| func         | Function   | Name of the implementation extending `VariableFunction`                                       |
| header       | Header     | Store key in HEADER after variable transformation, facilitating subsequent reads at the gateway layer. |

```json
{
  "name": "getUserByQuery",
  "scope": "QUERY",
  "key": "user",
  "func": "",
  "header": ""
}
```

## 4. Unit Rule

Unit rules define the traffic allocation rules between units and partitions.

Intra-city multi-active means there is only one unit, which can have multiple partitions.

| Attribute              | Name               | Description                                                                                  |
|------------------------|--------------------|----------------------------------------------------------------------------------------------|
| id                     | Unique ID          |                                                                                              |
| name                   | Name               |                                                                                              |
| liveType               | Multi-Active Type  | CROSS_REGION_LIVE: Unitized<br/>ONE_REGION_LIVE: Intra-city multi-active                     |
| variable               | Unit Routing Variable |                                                                                              |
| variableSource         | Default Variable Source |                                                                                              |
| variableFunction       | Variable Function  | Used to calculate the value and determine the unit by modulo, name of the implementation extending `UnitFunction` |
| variableMissingAction  | Action on Missing Variable | REJECT: Reject<br/>CENTER: Route to center                                                   |
| modulo                 | Modulo             | Determine the unit by modulo of the calculated value                                         |
| units                  | Unit Routing Rules |                                                                                              |

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

### 4.1 Unit Routing Rule

Unit routing rules describe unit routing information.

| Attribute    | Name         | Description                                                                                  |
|--------------|--------------|----------------------------------------------------------------------------------------------|
| code         | Unit Code    |                                                                                              |
| allows       | Allowed Routing Variable Whitelist | Equality check                                                                 |
| prefixes     | Allowed Routing Variable Prefix | Prefix check                                                                  |
| ranges       | Value Range  | Array, half-open interval of the modulo value, [start, end)                                 |
| cells        | Partitions   | Partition routing rules under the unit                                                       |

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

#### 4.1.1 Cell Routing Rule

Cell routing rules describe partition routing information.

| Attribute    | Name         | Description          |
|--------------|--------------|----------------------|
| code         | Partition Code |                      |
| allows       | Allowed Routing Variable Whitelist | Equality check       |
| prefixes     | Allowed Routing Variable Prefix | Prefix check         |
| weight       | Weight       | Weight between cells |

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

## 5. Live Domain

Live domains describe the domains enabled for multi-active, usually used for gateway interception of incoming traffic and routing to units according to matching unit rules.

| Attribute            | Name                | Description                                                                                  |
|----------------------|---------------------|----------------------------------------------------------------------------------------------|
| host                 | Domain              |                                                                                              |
| protocols            | Supported Protocols |                                                                                              |
| liveType             | Multi-Active Type   | CROSS_REGION_LIVE: Unitized<br/>ONE_REGION_LIVE: Intra-city multi-active                     |
| correctionType       | Correction Type     | UPSTREAM: Forward                                                                            |
| unitDomainEnabled    | Enable Unit Subdomain | If enabled, HTTP forwarding will be directed to the unit subdomain                           |
| unitDomains          | Unit Subdomains     | Unit subdomains for HTTP Web requests or callback closed-loop within the unit                |
| paths                | Path Rules          |                                                                                              |

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

### 5.1 Unit Subdomain

Unit subdomains describe the subdomains of the main domain in each unit.

| Attribute | Name        | Description                             |
|-----------|-------------|-----------------------------------------|
| unit      | Unit        |                                         |
| host      | Subdomain   | Default is `unit code-main domain`      |
| backend   | Backend Forwarding Address |                          |

```json
{
  "unit": "unit1",
  "host": "unit1-demo.live.local",
  "backend": ":8080"
}
```

### 5.2 Path Rules

Path rules describe unit rules matched on the path, starting with `/` and performing prefix matching according to the path segments. The longest matching path is selected.

For example, the request path `/mall/order/addOrder` matches the rules as follows:

1. `/mall/order/addOrder` matches, this rule is selected
2. `/mall/order` matches
3. `/mall` matches
4. `/mall/order/add` does not match
5. `/mall/or/addOrder` does not match

| Attribute             | Name                          | Description                                         |
|-----------------------|-------------------------------|-----------------------------------------------------|
| path                  | Path                          | Starts with `/`, performs prefix matching by path segments |
| ruleId                | Unit Rule ID                  |                                                     |
| customVariableSource  | Custom Variable Source Enabled | If not enabled, the default definition on the rule is used |
| variable              | Variable                      | Set when custom variable source is enabled          |
| variableSource        | Variable Source               | Set when custom variable source is enabled          |
| bizVariableEnabled    | Business Parameter Rule Enabled | Used for unified gateway, distinguished by parameters for important business |
| bizVariableName       | Business Parameter Name       |                                                     |
| bizVariableScope      | Business Parameter HTTP Scope | `QUERY`: Request parameter<br/>`HEADER`: Request header<br/>`COOKIE`: COOKIE |

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

#### 5.2.1 Business Parameter Rules

Business parameter rules match routing rules based on parameter values.

| Attribute | Name        | Description |
|-----------|-------------|-------------|
| value     | Parameter Value |         |
| ruleId    | Unit Rule ID |           |
| ruleName  | Unit Rule Name |         |

```json
{
  "value": "order",
  "ruleId": "1003",
  "ruleName": "Test"
}
```

## 6. Model Skeleton

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



