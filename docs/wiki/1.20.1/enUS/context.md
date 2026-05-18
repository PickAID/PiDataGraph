# Runtime Context

`PiGraphContext` is the public input for one graph execution. Numbers become expression variables. Objects are read by Java actions or predicates.

`PiEngineContext` remains as a compatibility alias for older integrations and the current action executor internals. New application-facing binders should use `PiGraphContext`.

## Java Key Constants

```java
public static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
public static final PiEngineContextKey<LivingEntity> TARGET =
        PiEngineContextKey.of("target", LivingEntity.class);
public static final PiEngineValueKey<Number> HUD_MANA_FILL =
        PiEngineValueKey.number("hud.mana_fill");
```

```java
PiGraphContext context = PiGraphContext.builder()
        .number(BASE_DAMAGE, 6)
        .object(TARGET, target)
        .build();
```

Rules:

- Context numbers must be expression variable names, such as `baseDamage`.
- Context objects also use plain variable names, such as `target`.
- Frame outputs may use plain names or dotted paths, such as `hud.mana_fill`.

## Read Results

```java
PiEngineFrame frame = action.execute(context);

double damage = frame.numberOr(PiEngineNumberKey.of("damage"), 0);
boolean accepted = frame.flagOr(PiEngineFlagKey.of("accepted"), false);
double manaFill = frame.numberOr(HUD_MANA_FILL, 0);
```

`emit_number`, `emit_flag`, and `emit_object` write to `PiEngineFrame`, so their `name` can be a dotted path:

```json
{
  "type": "pidatagraph:emit_number",
  "name": "hud.mana_fill",
  "value": "mana / maxMana"
}
```

## Minecraft Binding Helpers

For typed Java inputs, prefer `PiGraphContextBindings.record(...)` or a custom `PiGraphContextBinder`.

```java
public record SpellCastInput(Object caster, LivingEntity target, double baseDamage) {
}

PiGraphContextBinder<SpellCastInput> binder =
        PiGraphContextBindings.record("spell_cast", SpellCastInput.class);

PiGraphContext context = binder.bind(new SpellCastInput(caster, target, 6));
```

Legacy `PiEngineContextBindings` is still available for Minecraft-specific helper values:

| Method | Writes |
| --- | --- |
| `level` | `level`, `gameTime`, `dayTime`, `clientSide` |
| `random` | `random` object and expression `rand()` |
| `entity` | position, rotation, tick, onGround, delta |
| `living` | entity values plus health, maxHealth, absorption, armor |
| `vector` | `Vec3` object plus X/Y/Z/Length |
| `blockPos` | `BlockPos` object plus X/Y/Z |
| `itemStack` | `ItemStack` object plus count, damage, maxDamage, damageRatio, empty |
| `damageSource` | `damageSource` object |
| `hand` | `hand` object |

## Contract

Java actions and legacy binders declare inputs with `PiEngineContextContract`. New graph binders declare the same shape with `PiGraphContextSchema`.

```java
return PiEngineContextContract.builder()
        .number("baseDamage")
        .object("target", LivingEntity.class)
        .object("damageSource", DamageSource.class)
        .build();
```

```java
return PiGraphContextSchema.builder("spell_cast")
        .number("baseDamage")
        .object("target", LivingEntity.class)
        .object("damageSource", DamageSource.class)
        .build();
```

`PiEngineRunner` and `verify(...)` use contracts to report missing numbers, missing objects, and type mismatches before execution.
