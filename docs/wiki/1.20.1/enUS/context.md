# Context

`PiEngineContext` is the input for one execution. It stores numbers for expressions and objects for Java leaf actions.

Stable Java-side keys should be constants. Use `PiEngineNumberKey` for execution input numbers and `PiEngineContextKey<T>` for objects. Simple frame outputs can reuse `PiEngineNumberKey` / `PiEngineFlagKey`; dotted output paths such as `hud.mana_fill` should use `PiEngineValueKey`.

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
PiEngineValueKey<Number> HUD_MANA_FILL = PiEngineValueKey.number("hud.mana_fill");
PiEngineValueKey<Boolean> HUD_READY = PiEngineValueKey.flag("hud.ready");

boolean accepted = frame.flagOr(ACCEPTED, false);
double damage = frame.numberOr(DAMAGE, 0.0D);
int cooldown = frame.integerOr(COOLDOWN, 0);
Vec3 impact = frame.object(IMPACT).orElse(Vec3.ZERO);
double manaFill = frame.numberOr(HUD_MANA_FILL, 0.0D);
boolean ready = frame.flagOr(HUD_READY, false);
```

The distinction matters: context numbers become expression variables, so they must be plain names such as `baseDamage`; frame outputs are result paths and may use dotted grouping.

`emit_number`, `emit_flag`, and `emit_object` write frame outputs, so their `name` may also use dotted paths:

```json
{
  "type": "pidatagraph:emit_number",
  "name": "hud.mana_fill",
  "value": "resource / maxResource"
}
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
