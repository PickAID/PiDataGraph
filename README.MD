# PiDataGraph

[English](README_EN.md)

PiDataGraph 是给 Forge 1.20.1 用的数据驱动运行库。它把 JSON 里的公式和条件变成 Java 可以校验、编译和执行的对象。

它现在支持这些内容：

- 表达式：`PiDoubleExpression`、`PiIntExpression`、`PiBooleanExpression`
- 数据文件：`PiDataDefinition`、`PiDataSet`、`PiDataReloadListener`、`PiDataProvider`
- 执行输入和输出：`PiEngineContext`、`PiEngineContextBinder`、`PiEngineFrame`
- Java 侧公式组：`PiEngineFormulaSet`、`PiCompiledFormulaSet`
- Minecraft 数据包注册表：`PiDataPackRegistries.action(...)`、`PiEngineRunner`
- 生成入口：`@PiDataGraphModule`、`@PiDataPackRegistry`、`@PiGraphInput(facade = "...")`、`@PiGraphOutput`、`@PiOutput`
- 基础有向图执行：`PiGraphDefinition`、`PiGraphExecutor`
- 同步桥接：`PiDataGraphState`、`PiDataGraphSync`

推荐用注解生成 facade。`@PiDataPackRegistry` 会生成 Forge subscriber，自动注册 datapack registry 和 reload verifier。datagen、action 实现和普通玩法代码都可以依赖生成的 facade，不需要再手写一层 `_PiDataGraph` 包装。

当前版本：`com.mihono.pickaid:pidatagraph:0.0.5-dev`

## 文档

- [1.20.1 中文文档](docs/wiki/1.20.1/zhCN/README.md)
- [1.20.1 English docs](docs/wiki/1.20.1/enUS/README.md)

## Maven

使用 PickAID Maven 时，普通下游项目通常同时写 `implementation` 和 `jarjar`：

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", range = "[0.0.5,0.1.0)", transitive = false }

# 只有用 @PiDataGraphModule / @PiGraphInput / @PiGraphOutput 时才需要。
[dependencies.annotation_processor]
pidatagraph_processor = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }
```
