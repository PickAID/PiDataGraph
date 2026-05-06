# Runtime Context

`PiEngineContext` is the input environment for one action execution. Numbers become expression variables. Objects are passed to Java-side actions.

Normal gameplay code usually does not create `PiEngineContext` by hand. When using `@PiGraphInput(..., facade = "HitGraph")`, the generated `HitGraph.run(...)` binds `HitInput` into a context automatically.

## Generated Keys

Input record:

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

Output record:

```java
@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(
        @PiOutput("damage.final") double damage,
        boolean accepted,
        LivingEntity lastTarget
) {
}
```

This generates two key groups:

```java
HitGraph.Context.BASE;
HitGraph.Context.POWER;
HitGraph.Context.TARGET;
HitGraph.Context.DAMAGE_SOURCE;

HitGraph.Output.DAMAGE_FINAL;
HitGraph.Output.ACCEPTED;
HitGraph.Output.LAST_TARGET;
```

Do not hand-write `HitGraph.java` for these keys. The annotation processor generates them, and actions, datagen, and low-level tools can reference the generated class directly.

## Read Input in Actions

Custom actions read input from `PiEngineContext`, then write results into `PiEngineFrame`.

```java
@Override
public PiEngineFrame execute(PiEngineContext context) {
    double damage = context.number(HitGraph.Context.BASE)
            + context.number(HitGraph.Context.POWER) * 2;
    LivingEntity target = context.requireObject(HitGraph.Context.TARGET);

    return PiEngineFrame.builder()
            .number(HitGraph.Output.DAMAGE_FINAL, damage)
            .flag(HitGraph.Output.ACCEPTED, true)
            .object(HitGraph.Output.LAST_TARGET, target)
            .build();
}
```

Rules:

- Context numbers must be expression variable names, such as `base` and `power`.
- Context objects also use plain variable names, such as `target` and `damageSource`.
- Frame outputs may use plain names or dotted paths, such as `damage.final`.

## Read Output in Gameplay Code

With the generated facade, gameplay code receives the output record directly and does not need to touch `PiEngineFrame`.

```java
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);

if (output.accepted()) {
    target.hurt(source, (float) output.damage());
}
```

Only low-level runners or formula sets need direct frame reads:

```java
PiEngineFrame frame = runner.run(dataSet, HitGraph.id("fire_hit"), input);

double damage = frame.numberOr(HitGraph.Output.DAMAGE_FINAL, 0);
boolean accepted = frame.flagOr(HitGraph.Output.ACCEPTED, false);
LivingEntity lastTarget = frame.value(HitGraph.Output.LAST_TARGET);
```

## Minecraft Binding Helpers

`PiEngineContextBindings` is for low-level runners, tests, and project-specific binders. It expands Minecraft objects into objects plus common numbers.

```java
PiEngineContext context = PiEngineContextBindings.random(
        PiEngineContextBindings.living(
                PiEngineContext.builder(),
                HitGraph.Context.TARGET,
                target),
        level.random)
        .number(HitGraph.Context.BASE, 6)
        .number(HitGraph.Context.POWER, 3)
        .object(HitGraph.Context.DAMAGE_SOURCE, source)
        .build();
```

The example writes the `target` object and extra numbers such as `targetX`, `targetY`, `targetHealth`, and `targetArmor`. Expressions can use those numbers, but datagen and reload validation must also know they exist.

Note: those extra numbers only exist when you manually build a context through `PiEngineContextBindings`. Normal `HitGraph.run(level.registryAccess(), "fire_hit", input)` only binds the numbers and objects declared by the `HitInput` record. If datapack expressions need `targetHealth` directly, either pass it as a number field on `HitInput` or use a project-specific binder/runner path.

Supported bindings:

| Method | Writes |
| --- | --- |
| `level` | `level`, `gameTime`, `dayTime`, `clientSide` |
| `random` | `random` object and expression `rand(min, max)` |
| `entity` | entity object, position, rotation, tick, onGround, delta |
| `living` | entity values plus health, maxHealth, absorption, armor |
| `vector` | `Vec3` object plus X/Y/Z/Length |
| `blockPos` | `BlockPos` object plus X/Y/Z |
| `itemStack` | `ItemStack` object plus count, damage, maxDamage, damageRatio, empty |
| `damageSource` | `damageSource` object |
| `hand` | `hand` object |

## Contract

Custom actions use `PiEngineContextContract` to declare required inputs. Runners, reload verifiers, and datagen verifiers use it to catch missing keys early.

```java
@Override
public PiEngineContextContract contextContract() {
    return PiEngineContextContract.builder()
            .number(HitGraph.Context.BASE)
            .number(HitGraph.Context.POWER)
            .object(HitGraph.Context.TARGET)
            .object(HitGraph.Context.DAMAGE_SOURCE)
            .build();
}
```

Low-level validation can build a context with the same keys:

```java
PiDataBuildContext context = PiDataBuildContext.builder()
        .number(HitGraph.Context.BASE)
        .number(HitGraph.Context.POWER)
        .object(HitGraph.Context.TARGET)
        .object(HitGraph.Context.DAMAGE_SOURCE)
        .build();
```

Contracts can also query or remove object requirements with typed keys:

```java
PiEngineContextContract remaining = action.contextContract()
        .withoutObject(HitGraph.Context.TARGET);
```
