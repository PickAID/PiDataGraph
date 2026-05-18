# PiDataGraph

[English](README_EN.md)

PiDataGraph 是给 Forge 1.20.1 用的数据驱动运行库。它把 JSON 里的公式、条件和动作链变成 Java 可以校验、编译和执行的对象。

它现在支持这些内容：

- 表达式：`PiDoubleExpression`、`PiIntExpression`、`PiBooleanExpression`
- 数据文件：`PiDataDefinition`、`PiDataSet`、`PiDataReloadListener`、`PiDataProvider`
- 动作链：`PiEngineAction`、`PiEnginePredicate`、`PiEngineActionRegistry`
- 执行输入和输出：`PiGraphContext`、`PiGraphContextBinder`、`PiEngineFrame`
- 结构化图结果：`PiGraphResult`、`PiActionResult`、`PiDebugFrame`
- Java 侧公式组：`PiEngineFormulaSet`、`PiCompiledFormulaSet`
- Minecraft 数据包注册表：`PiDataPackRegistries.action(...)`、`PiEngineRunner`
- 基础有向图执行：`PiGraphDefinition`、`PiGraphExecutor`
- 同步桥接：`PiDataGraphState`、`PiDataGraphSync`

当前版本：`com.mihono.pickaid:pidatagraph:0.0.6`

## 文档

- [1.20.1 中文文档](docs/wiki/1.20.1/zhCN/README.md)
- [1.20.1 English docs](docs/wiki/1.20.1/enUS/README.md)

## Maven

使用 PickAID Maven 时，普通下游项目通常同时写 `implementation` 和 `jarjar`：

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.6", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.6", range = "[0.0.6,0.1.0)", transitive = false }
```
