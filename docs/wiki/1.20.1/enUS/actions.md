# Actions

Action chains move configurable flow into datapack JSON. A chain can derive temporary numbers, check predicates, branch, loop, and call project-defined Java leaf actions. Java leaf actions still perform world interaction, such as damage, movement, particles, cache writes, and resource changes.

## A Complete Action File

Example path:

```text
data/examplemod/spell_actions/fire_hit.json
```

```json
{
  "type": "pidatagraph:with_context",
  "numbers": {
    "finalDamage": "baseDamage + spellPower * 2 - targetArmor",
    "manaAfterCast": "mana - cost"
  },
  "objects": {
    "target": "hitEntity"
  },
  "child": {
    "type": "pidatagraph:guard",
    "predicate": "manaAfterCast >= 0 & finalDamage > 0",
    "child": {
      "type": "pidatagraph:sequence",
      "children": [
        {
          "type": "examplemod:damage",
          "amount": "finalDamage"
        },
        {
          "type": "pidatagraph:emit_number",
          "name": "damage",
          "value": "finalDamage"
        },
        {
          "type": "pidatagraph:emit_flag",
          "name": "accepted",
          "predicate": "manaAfterCast >= 0"
        }
      ]
    }
  }
}
```

This file does four things:

- `with_context` computes `finalDamage` and `manaAfterCast`, then aliases `hitEntity` to `target`.
- `guard` runs the child only when the actor can pay the cost and the hit has positive damage.
- `examplemod:damage` is a project action registered in Java.
- `emit_number` and `emit_flag` return values in `PiEngineFrame`.

## Executing an Action

At runtime, resolve the action and provide the input context for this execution.

```java
PiEngineFlagKey ACCEPTED = PiEngineFlagKey.of("accepted");
PiEngineNumberKey DAMAGE = PiEngineNumberKey.of("damage");
PiEngineNumberKey COOLDOWN = PiEngineNumberKey.of("cooldown");

PiEngineAction fireHit = spellActions.require(new ResourceLocation("examplemod", "fire_hit"));

PiEngineContext context = PiEngineContextBindings.itemStack(
        PiEngineContext.builder(),
        "weapon",
        player.getMainHandItem())
        .number("baseDamage", 4)
        .number("spellPower", spellPower)
        .number("targetArmor", target.getArmorValue())
        .number("mana", currentMana)
        .number("cost", 8)
        .object("actor", player)
        .object("hitEntity", target)
        .object("damageSource", damageSource)
        .build();

PiEngineFrame frame = fireHit.execute(context);

if (frame.flagOr(ACCEPTED, false)) {
    double damage = frame.numberOr(DAMAGE, 0.0D);
    int cooldown = frame.integerOr(COOLDOWN, 0);
}
```

`PiEngineContext` is input. `PiEngineFrame` is output. The caller decides which event, entity, item, block entity, UI preview, or runtime object provides the input values.
This example uses `guard`, so a rejected action returns an empty frame. Check key presence before reading optional frame values.

## Built-In Actions

| type | Required fields | Optional fields | Behavior |
| --- | --- | --- | --- |
| `pidatagraph:sequence` | `children` |  | Runs child actions in order and merges their frames. |
| `pidatagraph:guard` | `predicate`, `child` |  | Runs child only when predicate passes. |
| `pidatagraph:if` | `condition` | `then`, `else` | Branches to one child. Missing branches return an empty frame. |
| `pidatagraph:repeat` | `times`, `child` | `index` | Runs child repeatedly. Negative `times` becomes zero. |
| `pidatagraph:for_each_object` | `list`, `item`, `child` | `index` | Iterates an `Iterable` object from context. |
| `pidatagraph:with_number` | `name`, `value`, `child` |  | Derives one temporary number for child. |
| `pidatagraph:with_context` | `child` | `numbers`, `objects` | Derives several numbers or aliases existing objects for child. |
| `pidatagraph:emit_number` | `name`, `value` |  | Writes an expression result into the frame. |
| `pidatagraph:emit_flag` | `name`, `predicate` |  | Writes a boolean predicate result into the frame. |
| `pidatagraph:emit_object` | `name`, `source` |  | Copies an object from context into the frame. |

`sequence` rejects duplicate frame keys. If two steps need overwrite semantics, put that rule in a project Java action instead of emitting the same name twice.

## Built-In Predicates

A predicate can be a plain expression string:

```json
"mana >= cost & cooldown == 0"
```

It can also be an object with a `type` field:

```json
{
  "type": "pidatagraph:number_range",
  "key": "distance",
  "max": "range"
}
```

| type | Fields | Behavior |
| --- | --- | --- |
| string |  | Boolean expression. Values greater than `0.5` count as true. |
| `pidatagraph:all` | `predicates` | All children must pass. |
| `pidatagraph:any` | `predicates` | At least one child must pass. |
| `pidatagraph:not` | `predicate` | Negates one predicate. |
| `pidatagraph:chance` | `chance` | Random chance check. `chance` is an expression. |
| `pidatagraph:has_number` | `key` | Checks whether a number exists in context. |
| `pidatagraph:has_object` | `key` | Checks whether an object exists in context. |
| `pidatagraph:number_range` | `key`, `min`, `max` | Checks a number range. |
| `pidatagraph:item_enchantment` | `stack`, `enchantment`, `min`, `max` | Checks an enchantment level on an `ItemStack`. |
| `pidatagraph:item_stack` | `stack`, `item`, `tag`, `empty`, `min_count`, `max_count`, `min_damage`, `max_damage`, `max_damage_ratio` | Checks item, tag, count, and durability. |

Item predicates require an `ItemStack` object in context. Add it manually or use `PiEngineContextBindings.itemStack(...)`.

## Registering a Java Leaf Action

Project actions connect data to Minecraft APIs. This action reads `target` and `damageSource` from context, evaluates `amount`, and applies damage.

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
    public PiEngineFrame execute(PiEngineContext context) {
        LivingEntity target = context.object("target", LivingEntity.class).orElseThrow();
        DamageSource source = context.object("damageSource", DamageSource.class).orElseThrow();
        target.hurt(source, (float) context.evaluate(amount));
        return PiEngineFrame.empty();
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder()
                .object("target", LivingEntity.class)
                .object("damageSource", DamageSource.class)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineAction.super.verify(context, path);
        PiEngineActions.verify(path + ".amount", context, amount);
    }
}
```

Register the action registry:

```java
public static final PiEngineActionRegistry SPELL_ACTIONS = PiEngineActionRegistry.builder()
        .install(PiEngineActions.core())
        .installPredicates(PiEnginePredicates.core())
        .add(DamageAction.TYPE)
        .build();
```

If a custom action contains child actions, use `PiEngineActionType.of(id, (actionCodec, predicateCodec) -> ...)` so the Codec can decode nested actions.

## Loading Actions From Datapacks

`PiEngineActionData` provides the standard definition. The `folder` value controls the datapack path.

```java
public static final PiDataDefinition<PiEngineAction> SPELL_ACTION_DATA =
        PiEngineActionData.definition(
                new ResourceLocation("examplemod", "spell_actions"),
                "spell_actions",
                SPELL_ACTIONS);
```

This reads:

```text
data/<namespace>/spell_actions/<path>.json
```

Compile loaded data into a runtime library:

```java
PiEngineLibrary<PiEngineAction> spellActions = PiEngineLibrary.compile(
        loadedActions,
        PiEngineBuildContext.standard()
                .withContextContract(PiEngineContextContract.builder()
                        .number("baseDamage")
                        .number("spellPower")
                        .number("targetArmor")
                        .number("mana")
                        .number("cost")
                        .object("hitEntity", LivingEntity.class)
                        .object("damageSource", DamageSource.class)
                        .build()),
        PiEngineActionData.contentType(
                SPELL_ACTION_DATA,
                PiExpressionScope.of("baseDamage", "spellPower", "targetArmor", "mana", "cost")));
```

Bad expressions, missing objects, and type mismatches fail during load with paths such as:

```text
examplemod:fire_hit/root.child.children.0.amount: invalid expression `finalDamage + missing`
```

## Rules That Matter

- Context keys must be valid expression variable names, such as `finalDamage` or `mana_after`. Do not use dotted names such as `spell.damage` as context keys.
- `with_context.objects` maps “new name -> old name”. `"target": "hitEntity"` means the child reads `target`, using the parent context object named `hitEntity`.
- `for_each_object.list` must be an `Iterable`, such as `List<LivingEntity>`.
- `emit_object.source` copies an existing object. It does not create one.
- Java leaf actions should implement `contextContract()`. Without it, load-time validation cannot catch missing objects.
- Datapack JSON should compose flow. Java should call Minecraft APIs. Do not hide world mutation in expression functions.
