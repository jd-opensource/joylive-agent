# joylive-agent

English | [简体中文](./README-zh.md)

## Overview

Java bytecode enhancement framework for traffic governance in multi-activity (unit) scenarios. Following the traditional sdk governance mode and sidecar governance mode, the exploration and implementation of a new generation of proxyless mode provides a traffic governance framework with high performance, low resource consumption, cost reduction and efficiency for the enterprise java ecosystem.

## Architecture
1. Agent for multi-Live
![pic](docs/image/architect-0.png)

2. Agent architect
![pic](docs/image/architect-1.png)

3. Agent government theory
![pic](docs/image/architect-2.png)

4. Agent for full chain gray release based on lane
![pic](docs/image/architect-3.png)

5. Agent for local cell priority strategy
![pic](docs/image/architect-4.png)

## How to use

### Requirements

Compile requirement: JDK 8+ and Maven 3.2.5+ 

## Main Features

1. Supports traffic control for various models, including in-region multi-activity and cross-region multi-activity.
2. Supports full-link grayscale based on lanes.
3. Supports local cell priority and cross-cell fault-tolerance strategies.
4. Employs bytecode enhancement technology, which is non-intrusive to business code and minimally impacts business performance.
5. Adopts a microkernel architecture with strong class isolation, featuring an easy-to-use and simple extension and configuration system.

## Quick Start

View [Quick Start](./docs/quickstart.md)。

## Configuration reference manual

View [Configuration Reference Manual](./docs/config.md)。

## Usage Examples

View [Usage Examples](./docs/example.md)。

## Q&A

View [Q&A](./docs/qa.md)。

## Release History

View [Release History](./RELEASE.md)。