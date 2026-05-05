# Context

`PiEngineContext` is the input for one execution. It stores numbers for expressions and objects for Java leaf actions.

Stable Java-side keys should be constants. Use `PiEngineNumberKey` for numbers, `PiEngineContextKey<T>` for objects, and `PiEngineFlagKey` for boolean frame outputs. `PiEngineFrame` can reuse those keys when reading outputs:

```java
public static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
public static final PiEngineNumberKey RESOURCE = PiEngineNumberKey.of("resource");
public static final PiEngineNumberKey COST = PiEngineNumberKey.of("cost");
public static final PiEngineContextKey<Player> ACTOR = PiEngineContextKey.of("actor", Player.class);
public static final PiEngineContextKey<LivingEntity> TARGET = PiEngineContextKey.of("target", LivingEntity.class);
public static final PiEngineContextKey<DamageSource> DAMAGE_SOURCE = PiEngineContextKey.of("damageSource", DamageSource.class);

PiEngineContext context = PiEngineContext.builder()
        .number(BASE_DAMAGE, baseDamage)
        .number(RESOURCE, currentMana)
        .number(COST, manaCost)
        .object(ACTOR, player)
        .object(TARGET, target)
        .object(DAMAGE_SOURCE, damageSource)
        .build();
```

JSON still uses variable names such as `"baseDamage"` and `"resource"`. The key constants keep Java binding and frame reads from repeating raw strings.

Frame reads can reuse the same keys:

```java
PiEngineFlagKey ACCEPTED = PiEngineFlagKey.of("accepted");
PiEngineNumberKey DAMAGE = PiEngineNumberKey.of("damage");
PiEngineNumberKey COOLDOWN = PiEngineNumberKey.of("cooldown");
PiEngineContextKey<Vec3> IMPACT = PiEngineContextKey.of("impact", Vec3.class);

boolean accepted = frame.flagOr(ACCEPTED, false);
double damage = frame.numberOr(DAMAGE, 0.0D);
int cooldown = frame.integerOr(COOLDOWN, 0);
Vec3 impact = frame.object(IMPACT).orElse(Vec3.ZERO);
```

Keys come from the Java caller, loop actions, `with_number`, or `with_context`.

`PiEngineContextBindings` reduces repeated Minecraft setup code:

```java
PiEngineContext context = PiEngineContextBindings.itemStack(
        PiEngineContextBindings.vector(PiEngineContext.builder(), "impact", hit.getLocation()),
        "weapon",
        player.getMainHandItem())
        .number(BASE_DAMAGE, 6)
        .number(RESOURCE, mana)
        .number(COST, cost)
        .object("actor", player)
        .build();
```

This provides `impact`, `impactX`, `impactY`, `impactZ`, `impactLength`, `weapon`, `weaponCount`, `weaponDamage`, `weaponMaxDamage`, `weaponDamageRatio`, and `weaponEmpty`.

Leaf actions should declare required inputs with `PiEngineContextContract` and validate them during data load.
