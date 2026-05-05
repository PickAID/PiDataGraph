# PiDataGraph 1.20.1 Wiki

PiDataGraph 1.20.1 docs are split by usage:

- [Context](context.md): `PiEngineContext`, common keys, Minecraft object bindings.
- [Data](data.md): expressions, datapack JSON, datagen, validation.
- [Actions](actions.md): built-in actions, predicates, custom Java leaf actions.
- [Sync](sync.md): syncing graph state through PiSerializeKit and PiNet.

## Shortest Path

1. Define data shapes with `PiDoubleExpression` / `PiBooleanExpression` fields.
2. Use `PiDataDefinition` to declare codec and validation.
3. Use `PiEngineContentType` or `PiEngineActionData` to compile or verify at load time.
4. At runtime, put the current numbers and objects into `PiEngineContext`; use `PiEngineNumberKey` and `PiEngineContextKey<T>` constants for stable Java-side keys.

```java
public static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
public static final PiEngineNumberKey POWER = PiEngineNumberKey.of("power");

PiEngineContext context = PiEngineContext.builder()
        .number(BASE_DAMAGE, 6)
        .number(POWER, 3)
        .number("resource", 20)
        .number("cost", 5)
        .object("actor", player)
        .object("target", target)
        .build();
```
