# PiDataGraph 1.20.1

PiDataGraph provides a compact data-driven runtime layer: Codec-backed JSON files, expressions for numbers and conditions, `PiEngineContext` for connecting data to game code, and runners for execution and validation.

## Pages

- [Data files](data.md)
- [Runtime context](context.md)
- [Actions and predicates](actions.md)
- [Annotation generation](annotations.md)
- [Datapack registries and runner](registry-runner.md)
- [Core graph execution](core-graph.md)
- [Sync bridge](sync.md)

## Shortest Path

1. Declare a datapack action registry with `@PiDataGraphModule` and `@PiDataPackRegistry`.
2. Declare runtime input and output with `@PiGraphInput(..., facade = "HitGraph")` and `@PiGraphOutput`.
3. The compiler generates Forge subscribers that register the datapack registry and reload verifier.
4. Generate default JSON with `HitGraph.dataSet()` and `HitGraph.validationContext()`.
5. Run with `HitGraph.run(level.registryAccess(), "entry_id", input)` and read the output record.

```java
HitInput input = new HitInput(base, power, target, source);
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);
```
