# joylive-agent

[![Build](https://github.com/jd-opensource/joylive-agent/actions/workflows/build.yml/badge.svg)](https://github.com/jd-opensource/joylive-agent/actions/workflows/build.yml)
![License](https://img.shields.io/github/license/jd-opensource/joylive-agent.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.jd.live/joylive-agent.svg?label=maven%20central)](https://search.maven.org/search?q=g:com.jd.live)
[![GitHub repo](https://img.shields.io/badge/GitHub-repo-blue)](https://github.com/jd-opensource/joylive-agent)
[![GitHub release](https://img.shields.io/github/release/jd-opensource/joylive-agent.svg)](https://github.com/jd-opensource/joylive-agent/releases)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/jd-opensource/joylive-agent.svg)](http://isitmaintained.com/project/jd-opensource/joylive-agent "Percentage of issues still open")
[![Slack Status](https://img.shields.io/badge/slack-join_chat-white.svg?logo=slack&style=social)](https://joylivehq.slack.com)

<img src="docs/image/weixin.png" title="该二维码有效期截止到2024/7/8" width="150"  />

[English](./README.md) | 简体中文

## 概述

服务治理框架，支持传统的微服务治理、泳道治理和多活（单元化）流量治理。
继传统SDK治理模式与Sidecar治理模式之后，基于微内核可扩展架构的新一代Proxyless模式的探索实现，为企业Java生态提供高性能，低资源损耗，降本增效的流量治理框架。

## 架构
1. Agent在多活场景应用   
   ![pic](docs/image/architect-0.png)

2. Agent架构图   
   ![pic](docs/image/architect-1.png)

3. Agent治理原理   
   ![pic](docs/image/architect-2.png)

4. Agent基于泳道的全链路灰度   
   ![pic](docs/image/architect-3.png)

5. Agent本地分区优先策略   
   ![pic](docs/image/architect-4.png)

6. 更多请参考[架构手册](docs/cn/architect.md)

## 关联项目

1. [joylive-injector](https://github.com/jd-opensource/joylive-injector)，用于云原生场景自动注入`joylive-agent`

## 如何使用

### 需求

编译需求: JDK 17+ 与 Maven 3.2.5+
运行需求: JDK 8+

## 主要特性

1. 支持同城多活、异地多活等多种模型的流量控制；
2. 支持基于泳道的全链路灰度，QPS与并发限流，标签路由，负载均衡等微服务治理策略；
3. 支持分区本地优先和跨分区容错策略；
4. 采用字节码增强技术，对业务代码无侵入，业务性能影响最小；
5. 采用微内核架构，强类隔离，简单易用的扩展和配置体系。

## 快速开始

查看 [快速开始](./docs/cn/quickstart.md)

## 配置参考手册

查看 [配置参考手册](./docs/cn/config.md)

## 使用示例

查看 [使用示例](./docs/cn/example.md)

## 常见问题

查看 [常见问题](./docs/cn/qa.md)

## 发布历史

查看 [发布历史](./RELEASE-zh.md)

## 路线图

查看 [路线图](./docs/cn/roadmap.md)
