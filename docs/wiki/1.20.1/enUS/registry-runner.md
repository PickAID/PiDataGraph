# Datapack Registries and Runner

PiDataGraph can put actions into a Minecraft datapack registry. Runtime code reads data from `RegistryAccess`, so it does not need a custom reload cache.

See [Actions and predicates](actions.md) for built-in action and predicate JSON shapes.

## Recommended Path: Generated Facade

```java
@PiDataGraphModule(modid = "examplemod")
public final class ExampleGraphs {
    @PiDataPackRegistry(
            path = "hit_action",
            folder = "skills/hit",
            sync = PiDataPackSync.SYNC_TO_CLIENT)
    public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.builder()
            .install(PiEngineActions.core())
            .installPredicates(PiEnginePredicates.core())
            .add(DamageAction.TYPE)
            .build();
}

@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(
        double base,
        double power,
        LivingEntity target,
        @PiObject("damageSource") DamageSource source) {
}

@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(@PiOutput("damage.final") double damage, boolean accepted, LivingEntity lastTarget) {
}
```

The declaration above generates `HitGraph`. The annotation processor creates this class; you do not write it by hand. If IDEA does not recognize it immediately after adding the annotations, run `./gradlew compileJava` once or reload Gradle.

Common entry points live on `HitGraph`:

```java
HitGraph.id("fire_hit");
HitGraph.key("fire_hit");
HitGraph.dataSet();
HitGraph.dataSet("other_namespace");
HitGraph.validationContext();
HitGraph.run(level.registryAccess(), "fire_hit", input);
HitGraph.run(dataSet, "fire_hit", input);
```

### ID Methods

| Method | When to use it |
| --- | --- |
| `id("fire_hit")` | Use when an API only needs a resource id. The result is `examplemod:fire_hit`. |
| `key("fire_hit")` | Use when an API needs a datapack registry entry key. Sync, registry APIs, and `ResourceKey<?>` parameters use this. |

### Datagen Methods

| Method | When to use it |
| --- | --- |
| `dataSet()` | Use when generating default JSON for the current mod. |
| `dataSet("other_namespace")` | Use when generating JSON for another namespace. |
| `validationContext()` | Use during datagen or manual verification to tell the verifier which input numbers and objects exist. |

### Runtime Methods

| Method | When to use it |
| --- | --- |
| `run(level.registryAccess(), "fire_hit", input)` | Use at server runtime to read and execute from the current datapack registry. |
| `run(dataSet, "fire_hit", input)` | Use in tests, datagen tools, or no-world contexts with a temporary data set. |

### Event Methods

| Method | When to use it |
| --- | --- |
| `registerDatapackRegistry(event)` | Manually registers the datapack registry. Normal projects let the generated MOD bus subscriber call it. |
| `addReloadVerifier(event)` | Manually attaches reload validation. Normal projects let the generated FORGE bus subscriber call it. |

The generated Forge subscriber wires the datapack registry and reload verifier by default. `HitGraph.registerDatapackRegistry(event)` and `HitGraph.addReloadVerifier(event)` still exist, but normal projects do not call them directly.

Input keys live under `HitGraph.Context`; output keys live under `HitGraph.Output`:

```java
HitGraph.Context.BASE;
HitGraph.Context.POWER;
HitGraph.Context.TARGET;
HitGraph.Context.DAMAGE_SOURCE;
HitGraph.Output.DAMAGE_FINAL;
HitGraph.Output.ACCEPTED;
HitGraph.Output.LAST_TARGET;
```

Action contracts do not need stringly typed keys:

```java
@Override
public PiEngineContextContract contextContract() {
    return PiEngineContextContract.builder()
            .number(HitGraph.Context.BASE)
            .object(HitGraph.Context.TARGET)
            .object(HitGraph.Context.DAMAGE_SOURCE)
            .build();
}
```

Built-in emit actions use facade keys too:

```java
new PiEmitNumberAction(
        HitGraph.Output.DAMAGE_FINAL,
        PiDoubleExpression.of("base + power * 2"));
new PiEmitObjectAction(
        HitGraph.Output.LAST_TARGET,
        HitGraph.Context.TARGET);
```

The processor generates Forge event wiring. Normal projects keep the annotation declarations and do not add listeners manually in the mod constructor.

Default datagen:

```java
PiDataSet<PiEngineAction> defaults = HitGraph.dataSet()
        .entry("fire_hit", new DamageAction(PiDoubleExpression.of("base + power * 2")))
        .build();

new PiDataProvider(output, "ExampleMod Hit Action Data", HitGraph.validationContext(), defaults);
```

Use parameterless `dataSet()` for your own mod namespace. Use `dataSet(namespace)` when generating entries for another namespace.

Runtime:

```java
HitOutput output = HitGraph.run(
        level.registryAccess(),
        "fire_hit",
        new HitInput(base, power, target, source));
```

`fire_hit` is a data entry id. The processor does not generate business methods such as `fireHit(...)` for datapack files because entries can be added, removed, or renamed by other packs.

## Input and Output Rules

Input records:

- Primitive numbers and `Number` subclasses become context numbers.
- Other reference types become context objects.
- `@PiNumber("key")` and `@PiObject("key")` override context keys.
- The generated binder rejects null objects and null `Number` values.

Output records:

- Number components become `PiEngineValueKey<Number>`.
- Boolean components become `PiEngineValueKey<Boolean>`.
- Reference components become typed object value keys.
- `@PiOutput("key")` overrides frame value keys. Dotted output paths such as `damage.final` are supported.

One `@PiDataPackRegistry` can only have one `@PiGraphInput`. If `hit`, `projectile`, and `machine_tick` need different input records, declare multiple registry fields.

## Manual Registration

Without annotations, register an action datapack registry by hand:

```java
public static final ResourceKey<Registry<PiEngineAction>> HIT_ACTIONS_KEY =
        ResourceKey.createRegistryKey(new ResourceLocation("examplemod", "hit_actions"));

@SubscribeEvent
public static void newDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
    PiDataPackRegistries.action(
            event,
            HIT_ACTIONS_KEY,
            HIT_ACTIONS,
            PiDataPackSync.SYNC_TO_CLIENT);
}
```

`PiDataPackSync.SYNC_TO_CLIENT` provides a network codec for the registry. `PiDataPackSync.SERVER_ONLY` registers only the server codec.

Manual runners require a hand-written binder:

```java
public record HitInput(double base, double power, LivingEntity target, DamageSource source) {
}

public enum HitBinder implements PiEngineContextBinder<HitInput> {
    INSTANCE;

    @Override
    public PiEngineContextContract contract() {
        return PiEngineContextContract.builder()
                .number("base")
                .number("power")
                .object("target", LivingEntity.class)
                .object("damageSource", DamageSource.class)
                .build();
    }

    @Override
    public PiEngineContext bind(HitInput input) {
        return PiEngineContext.builder()
                .number("base", input.base())
                .number("power", input.power())
                .object("target", input.target())
                .object("damageSource", input.source())
                .build();
    }
}
```

Run:

```java
PiEngineRunner<HitInput> runner =
        PiEngineRunner.actionRegistry(HIT_ACTIONS_KEY, HitBinder.INSTANCE);

PiEngineFrame frame = runner.run(
        server.registryAccess(),
        ResourceKey.create(HIT_ACTIONS_KEY, new ResourceLocation("examplemod", "fire_hit")),
        input);
```

You can also run from a `PiDataSet<PiEngineAction>`:

```java
PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);
PiEngineFrame frame = runner.run(dataSet, new ResourceLocation("examplemod", "fire_hit"), input);
```

`runner.run(...)` verifies the binder contract and action contract before execution.

## Verify All Actions

```java
runner.verifyAll(dataSet);
runner.verifyAll(server.registryAccess());
```

Validation failures include the entry id and field path:

```text
examplemod:fire_hit/context.numbers.power: missing engine context number `power`
```
