# Annotation Generation

PiDataGraph annotations generate datapack registries, runners, context binders, typed keys, output mappers, and optional facades at compile time. There is no runtime annotation scan.

Set `facade` on `@PiGraphInput` for normal project code. The internal `_PiDataGraph` class is still generated, but it should not appear in gameplay calls, datagen, or action implementation examples.

## Gradle Setup

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", range = "[0.0.5,0.1.0)", transitive = false }

[dependencies.annotation_processor]
pidatagraph_processor = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }
```

With only `implementation` and `jarjar`, runtime classes are available, but annotation code is not generated.

## Where `HitGraph` Comes From

`HitGraph` is not a hand-written class, and it is not found by runtime scanning. The Java annotation processor generates it at compile time:

```java
@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(...) {
}
```

The `facade = "HitGraph"` value sets the generated class name. The class is generated in the same package as the `@PiDataGraphModule` class. If `ExampleGraphs` is in `com.examplemod.data`, the generated class is `com.examplemod.data.HitGraph`.

Do not create `HitGraph.java` yourself. If a source class with the same name exists, the processor reports a generation conflict.

## IntelliJ IDEA Recognition

IDEA can recognize the generated class, but Gradle must configure `annotationProcessor`. With this template, that is the `project.toml` entry:

```toml
[dependencies.annotation_processor]
pidatagraph_processor = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }
```

After a normal Gradle import, `HitGraph` appears under Gradle generated sources after compilation. If the class is still red after writing the annotations:

1. Run `./gradlew compileJava` or `./gradlew test`.
2. Reload All Gradle Projects in IDEA.
3. Check that the project has `annotation_processor`, not only `implementation` / `jarjar`.

If `HitGraph` and the caller are in the same package, no import is needed. Otherwise, import it like any normal Java class using its generated package.

## Complete Declaration

```java
@PiDataGraphModule(modid = "examplemod")
public final class ExampleGraphs {
    @PiDataPackRegistry(path = "hit_action", folder = "skills/hit", sync = PiDataPackSync.SYNC_TO_CLIENT)
    public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();

    private ExampleGraphs() {
    }
}

@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(
        double base,
        @PiNumber("power") Number attackPower,
        @PiObject("target") LivingEntity target,
        DamageSource damageSource
) {
}

@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(
        @PiOutput("damage.final") double damage,
        boolean accepted,
        LivingEntity lastTarget
) {
}
```

This declaration generates directly usable `HitGraph`:

```java
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);
PiDataSet<PiEngineAction> defaults = HitGraph.dataSet()
        .entry("fire_hit", action)
        .build();
PiDataBuildContext context = HitGraph.validationContext();
```

Generated keys also live on the facade:

```java
HitGraph.Context.BASE;
HitGraph.Context.POWER;
HitGraph.Context.TARGET;
HitGraph.Context.DAMAGE_SOURCE;
HitGraph.Output.DAMAGE_FINAL;
HitGraph.Output.ACCEPTED;
HitGraph.Output.LAST_TARGET;
```

## Annotation Reference

| Annotation | Target | Purpose |
| --- | --- | --- |
| `@PiDataGraphModule` | class | Declares a generation root and the `modid` namespace. |
| `@PiDataPackRegistry` | `static final PiEngineActionRegistry` field | Generates datapack registry, data definition, automatic Forge subscribers, manual registration entries, and reload validation. |
| `@PiGraphInput` | record | Maps record components into runner input and context contract. |
| `@PiGraphOutput` | record | Maps frame output back into a record and generates output typed keys. |
| `@PiNumber` | input record component | Overrides the context number key. |
| `@PiObject` | input record component | Overrides the context object key. |
| `@PiOutput` | output record component | Overrides the frame value key. Dotted paths such as `damage.final` are supported. |

## Generated Facade Methods

`@PiGraphInput.facade` is the entry point normal code should depend on. The generated `HitGraph` members are grouped by purpose below.

### ID Methods

| Method | Purpose |
| --- | --- |
| `id(String path)` | Converts `"fire_hit"` to `examplemod:fire_hit`. Use it for datagen, logging, normal lookup, and APIs that only need `ResourceLocation`. |
| `key(String path)` | Converts `"fire_hit"` to a `ResourceKey<PiEngineAction>` inside the datapack registry. Use it for sync, registry APIs, and APIs that explicitly require `ResourceKey<?>`. |

### Datagen Methods

| Method | Purpose |
| --- | --- |
| `dataSet()` | Creates a data builder for the current mod namespace. Use it when generating JSON for your own mod. |
| `dataSet(String namespace)` | Creates a data builder for a specific namespace. Use it for compatibility packs or tests that write another namespace. |
| `validationContext()` | Returns the runner validation context. Pass it to `PiDataProvider` so datagen checks expressions, input keys, and action contracts before writing JSON. |

### Runtime Methods

| Method | Purpose |
| --- | --- |
| `run(RegistryAccess access, String path, HitInput input)` | Reads and executes from the current server datapack registry. Gameplay code usually uses this overload. |
| `run(RegistryAccess access, ResourceLocation id, HitInput input)` | Use when the caller already has a full id, for example from config or a sync payload. |
| `run(RegistryAccess access, ResourceKey<?> key, HitInput input)` | Use when the caller already has a registry entry key, for example when chaining with registry or sync APIs. |
| `run(PiDataSet<PiEngineAction> actions, String path, HitInput input)` | Executes from a temporary data set. Tests and no-world tools usually use this overload. |
| `run(PiDataSet<PiEngineAction> actions, ResourceLocation id, HitInput input)` | Executes from a temporary data set when the caller already has a full id. |
| `run(PiDataSet<PiEngineAction> actions, ResourceKey<?> key, HitInput input)` | Executes from a temporary data set when the caller already has an entry key. |

### Event Methods

| Method | Purpose |
| --- | --- |
| `registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event)` | Manual registration entry. Normal projects let the generated MOD bus subscriber call it. |
| `addReloadVerifier(AddReloadListenerEvent event)` | Manual reload-verifier entry. Normal projects let the generated FORGE bus subscriber call it. |

Normal projects do not need `modBus.addListener(...)` after declaring `@PiDataPackRegistry`. Manual event methods remain for tests, special loading flows, and projects that need to control event wiring themselves.

### Key Groups

| Member | Purpose |
| --- | --- |
| `Context` | Input keys such as `HitGraph.Context.BASE` and `HitGraph.Context.TARGET`. Use them in action contracts and context reads. |
| `Output` | Output keys such as `HitGraph.Output.DAMAGE_FINAL`. Use them in emit actions and frame reads. |

## Default Mapping

`@PiGraphInput` maps record components this way:

| Java type | Default mapping |
| --- | --- |
| primitive number, such as `int` or `double` | context number |
| `Number` subtype, such as `Integer` or `BigDecimal` | context number; the binder rejects null |
| non-primitive reference type | typed context object |

`@PiGraphOutput` maps record components this way:

| Java type | Default mapping |
| --- | --- |
| number type | `PiEngineValueKey<Number>` |
| `boolean` / `Boolean` | `PiEngineValueKey<Boolean>` |
| other reference type | typed object value key |

## Constraints

`@PiDataGraphModule` classes must not be private, and enclosing classes must not be private. `modid` must be a valid ResourceLocation namespace.

`@PiDataPackRegistry` fields must be `static final PiEngineActionRegistry`, must not be private, and must live inside a `@PiDataGraphModule` class. `path` must be a valid ResourceLocation path. `folder` must be a relative data folder.

`@PiGraphInput` and `@PiGraphOutput` can only annotate records. The record and its enclosing classes must not be private. Across packages, the record and object component types must be publicly accessible.

One datapack registry can only have one `@PiGraphInput`. If two flows need different input records, declare two `@PiDataPackRegistry` fields.

Context keys must be expression variable names. Output keys can be plain names or dotted paths. Duplicate keys, duplicate generated member names, and input/output member collisions fail at compile time.
