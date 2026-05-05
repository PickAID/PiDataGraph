# PiDataGraph

[中文](README.MD)

PiDataGraph is a data-driven runtime library for Forge 1.20.1. It turns formulas, predicates, and action chains from JSON into Java objects that can be validated, compiled, and executed.

It currently supports:

- Expressions: `PiDoubleExpression`, `PiIntExpression`, `PiBooleanExpression`
- Data files: `PiDataDefinition`, `PiDataSet`, `PiDataReloadListener`, `PiDataProvider`
- Action chains: `PiEngineAction`, `PiEnginePredicate`, `PiEngineActionRegistry`
- Runtime input and output: `PiEngineContext`, `PiEngineContextBinder`, `PiEngineFrame`
- Minecraft datapack registries: `PiDataPackRegistries.action(...)`, `PiEngineRunner`
- Basic directed graph execution: `PiGraphDefinition`, `PiGraphExecutor`
- Sync bridge: `PiDataGraphState`, `PiDataGraphSync`

Current version: `com.mihono.pickaid:pidatagraph:0.0.4-dev`

## Docs

- [1.20.1 Chinese docs](docs/wiki/1.20.1/zhCN/README.md)
- [1.20.1 English docs](docs/wiki/1.20.1/enUS/README.md)

## Maven

With the PickAID Maven, downstream mods usually declare both `implementation` and `jarjar`:

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.4-dev", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.4-dev", range = "[0.0.4,0.1.0)", transitive = false }
```
