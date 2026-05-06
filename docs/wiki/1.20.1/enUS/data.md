# Data Files

The main PiDataGraph path is: declare datapack action data with annotations, then let the compiler generate a facade that project code can call directly. Gameplay code, datagen, and action implementations should depend on that facade instead of writing another wrapper around `_PiDataGraph`.

When `@PiGraphInput` uses `facade = "HitGraph"`, compilation generates `HitGraph`. `ExampleGraphs_PiDataGraph` still exists, but it is generated glue, not the application-facing entry point.

## Final JSON

This path comes from `@PiDataPackRegistry(folder = "skills/hit")`:

```text
data/examplemod/skills/hit/fire_hit.json
```

The file contains an action chain:

```json
{
  "type": "pidatagraph:sequence",
  "children": [
    {
      "type": "pidatagraph:emit_number",
      "name": "damage.final",
      "value": "base + power * 2"
    },
    {
      "type": "pidatagraph:emit_flag",
      "name": "accepted",
      "predicate": "base > 0 & power > 0"
    },
    {
      "type": "pidatagraph:emit_object",
      "name": "lastTarget",
      "source": "target"
    }
  ]
}
```

JSON describes data only: action types, expressions, input keys, and output keys. Java provides the input object, registers action types, runs the entry, and reads the output.

## Declare the Registry

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
            .build();

    private ExampleGraphs() {
    }
}
```

`path = "hit_action"` is the datapack registry id. `folder = "skills/hit"` is the JSON folder. If `folder` is omitted, `path` is used.

## Declare Input and Output

`HitInput` provides the values for one action run. `facade = "HitGraph"` generates the stable entry class.

```java
@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(
        double base,
        double power,
        LivingEntity target,
        @PiObject("damageSource") DamageSource source
) {
}
```

`HitOutput` declares the values read back after the action runs.

```java
@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(
        @PiOutput("damage.final") double damage,
        boolean accepted,
        LivingEntity lastTarget
) {
}
```

## Automatic Wiring

`@PiDataPackRegistry` makes the processor generate Forge subscribers. Normal projects do not need to write `modBus.addListener(...)` in the mod constructor.

Generated code does two things automatically:

- Registers the datapack registry during `DataPackRegistryEvent.NewRegistry` on the MOD bus.
- Adds the reload verifier on the FORGE bus, checking every action's input contract and expressions after reload.

`HitGraph.registerDatapackRegistry(event)` and `HitGraph.addReloadVerifier(event)` are still generated, but they are low-level entries. Call them directly only in tests, special loading flows, or projects that intentionally avoid the automatic subscriber.

## `HitGraph` Facade Method List

### ID Methods

| Method | Purpose |
| --- | --- |
| `HitGraph.id("fire_hit")` | Returns the full resource id: `examplemod:fire_hit`. Use it for datagen, logs, config reads, and normal lookup. |
| `HitGraph.key("fire_hit")` | Returns the datapack registry entry key. Use it with registry APIs or sync APIs that need `ResourceKey<?>`. |

### Datagen Methods

| Method | Purpose |
| --- | --- |
| `HitGraph.dataSet()` | Creates a data-set builder for the current mod namespace. Use it when generating default JSON for your own mod. |
| `HitGraph.dataSet("other_namespace")` | Creates a data-set builder for a specific namespace. Use it for compatibility packs, test data, or another namespace. |
| `HitGraph.validationContext()` | Creates the validation context. Pass it to `PiDataProvider` so file generation checks expressions, input keys, action contracts, and output keys. |

### Runtime Methods

| Method | Purpose |
| --- | --- |
| `HitGraph.run(level.registryAccess(), "fire_hit", input)` | Normal server gameplay call. Reads and executes from the current datapack registry. |
| `HitGraph.run(level.registryAccess(), id, input)` | Use when the caller already has a `ResourceLocation`, for example from config, another registry, or network sync. |
| `HitGraph.run(level.registryAccess(), key, input)` | Use when the caller already has a `ResourceKey<?>`, for example when chaining with registry APIs. |
| `HitGraph.run(dataSet, "fire_hit", input)` | Use in tests, datagen tools, and no-world contexts. Executes from a temporary data set. |
| `HitGraph.run(dataSet, id, input)` | Executes from a temporary data set when the caller already has a full `ResourceLocation`. |
| `HitGraph.run(dataSet, key, input)` | Executes from a temporary data set when the caller already has a registry entry key. |

### Event Methods

| Method | Purpose |
| --- | --- |
| `HitGraph.registerDatapackRegistry(event)` | Manually registers the datapack registry. Normal projects let the generated Forge MOD bus subscriber call it. |
| `HitGraph.addReloadVerifier(event)` | Manually attaches reload validation. Normal projects let the generated Forge FORGE bus subscriber call it. |

Normal projects only declare annotations and use business-facing entries such as `HitGraph.run(...)` and `HitGraph.dataSet(...)`. Event methods remain for tests and special loading flows; they are not daily boilerplate.

## Custom Action

Actions use keys exposed by the facade. Input keys live under `HitGraph.Context`; output keys live under `HitGraph.Output`.

```java
public record DamageAction(PiDoubleExpression amount) implements PiEngineAction {
    public static final PiEngineActionType<DamageAction> TYPE = PiEngineActionType.of(
            new ResourceLocation("examplemod", "damage"),
            actionCodec -> RecordCodecBuilder.create(instance -> instance.group(
                    PiDoubleExpression.CODEC.fieldOf("amount").forGetter(DamageAction::amount)
            ).apply(instance, DamageAction::new)));

    @Override
    public PiEngineActionType<?> type() {
        return TYPE;
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder()
                .number(HitGraph.Context.BASE)
                .number(HitGraph.Context.POWER)
                .object(HitGraph.Context.TARGET)
                .build();
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        double damage = context.number(HitGraph.Context.BASE)
                + context.number(HitGraph.Context.POWER) * 2;
        return PiEngineFrame.builder()
                .number(HitGraph.Output.DAMAGE_FINAL, damage)
                .flag(HitGraph.Output.ACCEPTED, true)
                .object(HitGraph.Output.LAST_TARGET, context.requireObject(HitGraph.Context.TARGET))
                .build();
    }
}
```

## Generate Default Data

Datagen also uses only `HitGraph`:

```java
PiDataSet<PiEngineAction> defaults = HitGraph.dataSet()
        .entry("fire_hit",
                new PiSequenceAction(List.of(
                        new PiEmitNumberAction(
                                HitGraph.Output.DAMAGE_FINAL,
                                PiDoubleExpression.of("base + power * 2")),
                        new PiEmitFlagAction(
                                HitGraph.Output.ACCEPTED,
                                PiExpressionPredicate.of("base > 0 & power > 0")),
                        new PiEmitObjectAction(
                                HitGraph.Output.LAST_TARGET,
                                HitGraph.Context.TARGET))))
        .build();

gen.addProvider(server, new PiDataProvider(
        output,
        "ExampleMod Hit Action Data",
        HitGraph.validationContext(),
        defaults));
```

The second `PiDataProvider` argument is the datagen display name. It is used only for logs and task display; it does not define the registry id, file path, or namespace. `PiDataProvider` validates data before writing files. If an expression references a missing number such as `missingPower`, datagen fails.

## Run

```java
HitInput input = new HitInput(base, power, target, source);
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);

if (output.accepted()) {
    target.hurt(source, (float) output.damage());
}
```

`fire_hit` is a datapack entry id, not a Java method name. The processor does not generate business methods such as `fireHit(...)` for every data file because datapack entries can be added, removed, or renamed by players and packs. The stable generated entry is `HitGraph.run(..., "fire_hit", input)`.

If a project wants a shorter call, it can add its own gameplay method, but PiDataGraph does not require that wrapper as boilerplate.

## What Can Be Changed

Datapacks can change:

- `value` formulas, such as `"base + power * 3"`.
- `predicate` conditions, such as `"base > 0 & power >= 2"`.
- Action-chain structure, such as adding `pidatagraph:guard`, `pidatagraph:if`, or `pidatagraph:repeat`.
- Output keys, as long as the Java output record still reads those keys.

Do not freely change:

- Input keys such as `base` or `target`, unless the Java input record changes too.
- Custom action types, unless Java registered the type.
- Keys required by the output record, otherwise output reading fails.

## Expression Syntax

| Kind | Supported |
| --- | --- |
| Math | `+`, `-`, `*`, `/`, `%`, `^` |
| Comparison | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| Logic | `&`, `|` |

### Expression Function List

These functions are used inside expression strings, for example `"clamp(base + power * 2, 0, 40)"`.

| Function | Arguments | Purpose |
| --- | --- | --- |
| `min(a, b)` | Two numbers | Returns the smaller value. |
| `max(a, b)` | Two numbers | Returns the larger value. |
| `clamp(value, min, max)` | Value, lower bound, upper bound | Restricts `value` to the `[min, max]` range. |
| `abs(value)` | One number | Returns the absolute value. |
| `floor(value)` | One number | Rounds down. `floor(2.9)` is `2`. |
| `ceil(value)` | One number | Rounds up. `ceil(2.1)` is `3`. |
| `round(value)` | One number | Returns the nearest whole number, matching Java `Math.rint` behavior. |
| `sqrt(value)` | One number | Square root. |
| `pow(base, exponent)` | Base, exponent | Power. `pow(2, 3)` is `8`. |
| `sin(value)` | Radians | Sine. The argument is radians, not degrees. |
| `cos(value)` | Radians | Cosine. The argument is radians, not degrees. |
| `tan(value)` | Radians | Tangent. The argument is radians, not degrees. |
| `rand(min, max)` | Lower bound, upper bound | Returns a random number in `[min, max)`, using the current `PiEngineContext` random source. |

For boolean expressions, results greater than `0.5` count as true.

### Custom Expression Functions

Generated facades use the standard expression language by default, which means the functions listed above. Low-level runners, custom binders, and manual validation can extend it with `PiExpressionLanguage.standardBuilder()`, but datagen/reload validation and runtime must use the same language.

```java
PiExpressionLanguage language = PiExpressionLanguage.standardBuilder()
        .function("damage_scale", 2, (context, args) ->
                args[0] * context.variables().get("power") + args[1])
        .build();

PiDataBuildContext buildContext = PiDataBuildContext.builder()
        .expressionLanguage(language)
        .number(HitGraph.Context.POWER)
        .build();

PiEngineContext runtimeContext = PiEngineContext.builder()
        .language(language)
        .number(HitGraph.Context.POWER, 4)
        .build();
```

If a function is registered only at runtime but datagen/reload validation does not use the same language, generation or reload fails. That is intentional: invalid data should fail early.

## Low-Level JSON API

`PiDataDefinition`, `PiDataSet`, `PiDataProvider`, and `PiDataReloadListener` remain available. They fit plain JSON tables. For complex gameplay logic that needs actions, runners, input/output records, datapack registries, and reload validation, use the annotation system on this page.
