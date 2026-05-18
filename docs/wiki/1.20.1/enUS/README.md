# PiDataGraph 1.20.1

PiDataGraph provides a compact data-driven runtime layer: Codec-backed JSON files, expressions for numbers and conditions, action/predicate chains for flow, and `PiGraphContext` for connecting typed game inputs to graph execution.

## Pages

- [Data and expressions](data.md)
- [Runtime context](context.md)
- [Actions and predicates](actions.md)
- [Datapack registries and runner](registry-runner.md)
- [Core graph execution](core-graph.md)
- [Sync bridge](sync.md)

## Shortest Path

1. Use `PiDoubleExpression`, `PiIntExpression`, and `PiBooleanExpression` in your data model.
2. Declare the JSON folder, Codec, and validation with `PiDataDefinition`.
3. Load normal JSON with `PiDataReloadListener`, or register action data with `PiDataPackRegistries.action(...)`.
4. Bind the current event, entity, item, block entity, or runtime object into `PiGraphContext`.
5. Execute an action or compiled entry, then read results from `PiEngineFrame`.

```java
PiGraphContext context = PiGraphContext.builder()
        .number("baseDamage", 6)
        .number("spellPower", 3)
        .object("actor", player)
        .object("target", target)
        .build();
```
