# PiDataGraph

[English](README_EN.md)

PiDataGraph 把数据包里的公式、条件和动作链编译成 Java 可以直接执行的运行时逻辑。它适合做技能、机器、事件监听、UI 预览、配置驱动玩法和其他需要“数据决定执行流程”的系统。

当前主线版本文档：

- [1.20.1 中文 Wiki](docs/wiki/1.20.1/zhCN/README.md)
- [1.20.1 English Wiki](docs/wiki/1.20.1/enUS/README.md)

## 现在负责什么

- 表达式：`PiDoubleExpression`、`PiIntExpression`、`PiBooleanExpression`
- 数据加载和校验：`PiDataDefinition`、`PiDataSet`、`PiDataRuntimeLoader`
- 动作链：`PiEngineAction`、`PiEnginePredicate`、`PiEngineActionRegistry`
- 执行上下文：`PiEngineContext`、`PiEngineNumberKey`、`PiEngineFlagKey`、`PiEngineContextKey`、`PiEngineContextBindings`
- 运行时同步桥接：`PiDataGraphSync`，消费 PiSerializeKit 和 PiNet

README 只保留入口。具体怎么写 JSON、怎么注册 action、怎么做 datagen、怎么同步状态，都放在 versioned wiki 里维护。

## Maven

当前开发版本在 `project.toml` 中维护。下游通常通过 PickAID Maven 引入，并让模板处理 jar-in-jar：

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:<version>", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:<version>", range = "[<version>,0.1.0)", transitive = false }
```
