# PiDataGraph

[中文](README.MD)

PiDataGraph is a data-driven runtime library for Forge 1.20.1. It turns formulas and conditions from JSON into Java objects that can be validated, compiled, and executed.

It currently supports:

- Expressions: `PiDoubleExpression`, `PiIntExpression`, `PiBooleanExpression`
- Data files: `PiDataDefinition`, `PiDataSet`, `PiDataReloadListener`, `PiDataProvider`
- Runtime input and output: `PiEngineContext`, `PiEngineContextBinder`, `PiEngineFrame`
- Java-side formula sets: `PiEngineFormulaSet`, `PiCompiledFormulaSet`
- Minecraft datapack registries: `PiDataPackRegistries.action(...)`, `PiEngineRunner`
- Generated entry points: `@PiDataGraphModule`, `@PiDataPackRegistry`, `@PiGraphInput(facade = "...")`, `@PiGraphOutput`, `@PiOutput`
- Basic directed graph execution: `PiGraphDefinition`, `PiGraphExecutor`
- Sync bridge: `PiDataGraphState`, `PiDataGraphSync`

Use annotations to generate a facade. `@PiDataPackRegistry` generates Forge subscribers that register the datapack registry and reload verifier automatically. Datagen, action implementations, and gameplay code can depend on the generated facade without hand-writing a wrapper around `_PiDataGraph`.

Current version: `com.mihono.pickaid:pidatagraph:0.0.5-dev`

## Docs

- [1.20.1 Chinese docs](docs/wiki/1.20.1/zhCN/README.md)
- [1.20.1 English docs](docs/wiki/1.20.1/enUS/README.md)

## Maven

With the PickAID Maven, downstream mods usually declare both `implementation` and `jarjar`:

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", range = "[0.0.5,0.1.0)", transitive = false }

# Only needed when using @PiDataGraphModule / @PiGraphInput / @PiGraphOutput.
[dependencies.annotation_processor]
pidatagraph_processor = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }
```
