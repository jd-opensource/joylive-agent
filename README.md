# joylive-agent

[![Build](https://github.com/jd-opensource/joylive-agent/actions/workflows/build.yml/badge.svg)](https://github.com/jd-opensource/joylive-agent/actions/workflows/build.yml)
![License](https://img.shields.io/github/license/jd-opensource/joylive-agent.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.jd.live/joylive-agent.svg?label=maven%20central)](https://search.maven.org/search?q=g:com.jd.live)
[![GitHub repo](https://img.shields.io/badge/GitHub-repo-blue)](https://github.com/jd-opensource/joylive-agent)
[![GitHub release](https://img.shields.io/github/release/jd-opensource/joylive-agent.svg)](https://github.com/jd-opensource/joylive-agent/releases)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/jd-opensource/joylive-agent.svg)](http://isitmaintained.com/project/jd-opensource/joylive-agent "Percentage of issues still open")
[![Slack Status](https://img.shields.io/badge/slack-join_chat-white.svg?logo=slack&style=social)](https://joylivehq.slack.com)

<img src="docs/image/weixin.png" title="The QR code is valid until 2024/11/15" width="150" />

English | [简体中文](./README-zh.md)

## Overview

Service Governance Framework, supporting traditional microservice governance, swimlane governance, and multi-active (unit) traffic governance.
Following the traditional SDK governance model and Sidecar governance model, an exploration and implementation of the new generation Proxyless mode based on a microkernel extensible architecture, providing a high-performance, low-resource-consumption, cost-effective traffic governance framework for the enterprise Java ecosystem.

## Architecture
1. Agent for multi-live   
![pic](docs/image/architect-0.png)

2. Agent architect   
![pic](docs/image/architect-1.png)

3. Agent government theory   
![pic](docs/image/architect-2.png)

4. Agent for full chain gray release based on lane   
![pic](docs/image/architect-3.png)

5. Agent for local cell priority strategy   
![pic](docs/image/architect-4.png)

6. For more information, please refer to the [Architecture Manual](docs/architect.md).

## Related Projects

1. [joylive-injector](https://github.com/jd-opensource/joylive-injector), used for cloud-native scenario auto-injection of `joylive-agent`.

## How to use

### Requirements

Compile requirement: JDK 17+ and Maven 3.2.5+ 

Runtime requirement: JDK 8+

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

## Roadmap

View [Roadmap](./docs/roadmap.md)
