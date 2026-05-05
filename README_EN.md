# PiDataGraph

[中文](README.MD)

PiDataGraph compiles formulas, predicates, and action chains from data files into runtime Java logic. It is meant for skills, machines, event listeners, UI previews, config-driven gameplay, and other systems where data decides what runtime flow should do.

Current mainline documentation:

- [1.20.1 Chinese Wiki](docs/wiki/1.20.1/zhCN/README.md)
- [1.20.1 English Wiki](docs/wiki/1.20.1/enUS/README.md)

## What It Owns

- Expressions: `PiDoubleExpression`, `PiIntExpression`, `PiBooleanExpression`
- Data loading and validation: `PiDataDefinition`, `PiDataSet`, `PiDataRuntimeLoader`
- Action chains: `PiEngineAction`, `PiEnginePredicate`, `PiEngineActionRegistry`
- Runtime context: `PiEngineContext`, `PiEngineNumberKey`, `PiEngineFlagKey`, `PiEngineContextKey`, `PiEngineContextBindings`
- Sync bridge: `PiDataGraphSync`, consuming PiSerializeKit and PiNet

The README is intentionally short. JSON structure, action registration, datagen, validation, and sync examples are maintained in the versioned wiki.

## Maven

The current development version is maintained in `project.toml`. Downstream projects normally import it from the PickAID Maven and let the template handle jar-in-jar:

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:<version>", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:<version>", range = "[<version>,0.1.0)", transitive = false }
```
