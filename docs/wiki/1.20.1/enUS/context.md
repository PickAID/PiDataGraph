# Runtime Context

`PiEngineContext` is the input for one execution. Numbers become expression variables. Objects are read by Java actions or predicates.

## Java Key Constants

```java
public static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
public static final PiEngineContextKey<LivingEntity> TARGET =
        PiEngineContextKey.of("target", LivingEntity.class);
public static final PiEngineValueKey<Number> HUD_MANA_FILL =
        PiEngineValueKey.number("hud.mana_fill");
```

```java
PiEngineContext context = PiEngineContext.builder()
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

`PiEngineContextBindings` writes both objects and common numbers.

```java
PiEngineContext context = PiEngineContextBindings.itemStack(
        PiEngineContextBindings.living(
                PiEngineContext.builder(),
                "target",
                target),
        "weapon",
        player.getMainHandItem())
        .number("baseDamage", 6)
        .build();
```

Supported bindings:

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

Java actions and binders declare inputs with `PiEngineContextContract`.

```java
return PiEngineContextContract.builder()
        .number("baseDamage")
        .object("target", LivingEntity.class)
        .object("damageSource", DamageSource.class)
        .build();
```

`PiEngineRunner` and `verify(...)` use contracts to report missing numbers, missing objects, and type mismatches before execution.
