# joylive-agent

[![Build](https://github.com/jd-opensource/joylive-agent/actions/workflows/build.yml/badge.svg)](https://github.com/jd-opensource/joylive-agent/actions/workflows/build.yml)
![License](https://img.shields.io/github/license/jd-opensource/joylive-agent.svg)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/jd-opensource/joylive-agent.svg)](http://isitmaintained.com/project/jd-opensource/joylive-agent "Percentage of issues still open")

English | [简体中文](./README-zh.md)

## Overview

Java bytecode enhancement framework for traffic governance in multi-activity (unit) scenarios. Following the traditional sdk governance mode and sidecar governance mode, exploring the implementation of the new generation Proxyless mode based on microkernel extensible architecture, providing high performance, low resource consumption, and cost-effective traffic governance framework for enterprise Java ecosystem.

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

6. ClassLoader for isolation   
![pic](docs/image/architect-5.png)

## How to use

### Requirements

Compile requirement: JDK 8+ and Maven 3.2.5+ 

## Main Features

1. Supports traffic control for various models, including in-region multi-activity and cross-region multi-activity.
2. Support swimlane-based full-link gray scale, QPS and concurrent current limiting, label routing, load balancing and other microservice governance strategies;
3. Supports local cell priority and cross-cell fault-tolerance strategies.
4. Employs bytecode enhancement technology, which is non-intrusive to business code and minimally impacts business performance.
5. Adopts a microkernel architecture with strong class isolation, featuring an easy-to-use and simple extension and configuration system.

## Quick Start

View [Quick Start](./docs/quickstart.md)

## Configuration reference manual

View [Configuration Reference Manual](./docs/config.md)

## Usage Examples

View [Usage Examples](./docs/example.md)

## Q&A

View [Q&A](./docs/qa.md)

## Release History

View [Release History](./RELEASE.md)