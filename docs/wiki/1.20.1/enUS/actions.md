# Actions and Predicates

`PiEngineAction` describes configurable flow. JSON composes the flow. Java actions call Minecraft APIs.

## Complete Action JSON

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
          "name": "result.damage",
          "value": "finalDamage"
        },
        {
          "type": "pidatagraph:emit_flag",
          "name": "result.accepted",
          "predicate": "manaAfterCast >= 0"
        }
      ]
    }
  }
}
```

## Built-In Actions

| type | Fields | Behavior |
| --- | --- | --- |
| `pidatagraph:sequence` | `children` | Runs child actions in order and merges their frames |
| `pidatagraph:guard` | `predicate`, `child` | Runs child only when the predicate passes |
| `pidatagraph:if` | `condition`, `then`, `else` | Runs one branch |
| `pidatagraph:repeat` | `times`, `child`, `index` | Repeats child; `index` is optional |
| `pidatagraph:for_each_object` | `list`, `item`, `child`, `index` | Iterates an `Iterable` object from context |
| `pidatagraph:with_number` | `name`, `value`, `child` | Adds one derived number for child |
| `pidatagraph:with_context` | `numbers`, `objects`, `child` | Adds several numbers or object aliases |
| `pidatagraph:emit_number` | `name`, `value` | Writes a number frame output |
| `pidatagraph:emit_flag` | `name`, `predicate` | Writes a boolean frame output |
| `pidatagraph:emit_object` | `name`, `source` | Copies a context object into the frame |

`sequence` rejects duplicate frame keys.

## Built-In Predicates

A predicate can be a boolean expression string:

```json
"mana >= cost & cooldown == 0"
```

It can also be an object with a `type`:

```json
{
  "type": "pidatagraph:number_range",
  "key": "distance",
  "max": "range"
}
```

| type | Fields |
| --- | --- |
| string | boolean expression |
| `pidatagraph:all` | `predicates` |
| `pidatagraph:any` | `predicates` |
| `pidatagraph:not` | `predicate` |
| `pidatagraph:chance` | `chance` |
| `pidatagraph:has_number` | `key` |
| `pidatagraph:has_object` | `key` |
| `pidatagraph:number_range` | `key`, `min`, `max` |
| `pidatagraph:item_enchantment` | `stack`, `enchantment`, `min`, `max` |
| `pidatagraph:item_stack` | `stack`, `item`, `tag`, `empty`, `min_count`, `max_count`, `min_damage`, `max_damage`, `max_damage_ratio` |

`item_stack` and `item_enchantment` read an `ItemStack` object from context.

## Custom Java Action

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
        PiActionResult request = PiActionResult.request(
                new ResourceLocation("pidamage", "damage_request"),
                "impact_damage");
        return PiEngineFrame.builder()
                .number("impact_damage.amount", context.evaluate(amount))
                .object("impact_damage.result", request)
                .build();
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.empty();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineAction.super.verify(context, path);
        PiEngineActions.verify(path + ".amount", context, amount);
    }
}
```

PiDataGraph actions describe structured requests. The owning runtime, such as
PiEngine plus PiDamage, re-resolves target/source state and applies the request;
graph actions should not mutate the world directly.

Register the action registry:

```java
public static final PiEngineActionRegistry SPELL_ACTIONS = PiEngineActionRegistry.builder()
        .install(PiEngineActions.core())
        .installPredicates(PiEnginePredicates.core())
        .add(DamageAction.TYPE)
        .build();
```

Declare the data file type:

```java
public static final PiDataDefinition<PiEngineAction> SPELL_ACTION_DATA =
        PiEngineActionData.definition(
                new ResourceLocation("examplemod", "spell_actions"),
                "spell_actions",
                SPELL_ACTIONS);
```

Read path:

```text
data/<namespace>/spell_actions/<path>.json
```

Read the frame at runtime:

```java
PiEngineValueKey<Number> DAMAGE = PiEngineValueKey.number("result.damage");
PiEngineValueKey<Boolean> ACCEPTED = PiEngineValueKey.flag("result.accepted");

PiEngineFrame frame = action.execute(context);
double damage = frame.numberOr(DAMAGE, 0);
boolean accepted = frame.flagOr(ACCEPTED, false);
```
