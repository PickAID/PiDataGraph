# Actions and Predicates

Action JSON is decoded by `PiEngineActionRegistry`. Use `PiEngineActionRegistry.standard()` or install `PiEngineActions.core()` and `PiEnginePredicates.core()` in your own registry.

Every action object has a `type`. Predicates can either be a boolean expression string or an object with a `type`.

## Core Actions

| Type | Fields | Runtime behavior |
| --- | --- | --- |
| `pidatagraph:sequence` | `children` | Runs children in order and merges their frames. Duplicate output keys fail. |
| `pidatagraph:noop` | none | Does nothing and returns an empty frame. Useful for placeholders or disabled branches. |
| `pidatagraph:fail` | `message` | Throws an exception with `message` when this action is reached. |
| `pidatagraph:if` | `condition`, optional `then`, optional `else` | Runs one branch. Missing branch returns an empty frame. |
| `pidatagraph:repeat` | `times`, optional `index`, `child` | Runs `child` up to `times`; negative counts become `0`. |
| `pidatagraph:with_number` | `name`, `value`, `child` | Adds one derived number for `child`. |
| `pidatagraph:with_context` | optional `numbers`, optional `objects`, `child` | Adds derived numbers and remapped objects for `child`. |
| `pidatagraph:guard` | `predicate`, `child` | Runs `child` only when the predicate passes. |
| `pidatagraph:for_each_object` | `list`, `item`, optional `index`, `child` | Iterates an `Iterable` object, binding each element as `item`. |
| `pidatagraph:emit_number` | `name`, `value` | Emits a numeric frame value. |
| `pidatagraph:emit_random_number` | `name`, `min`, `max` | Emits a `[min, max)` number from the current context random source. |
| `pidatagraph:emit_flag` | `name`, `predicate` | Emits a boolean frame value. |
| `pidatagraph:emit_object` | `name`, `source` | Copies a context object into the frame. |
| `pidatagraph:select_object` | `name`, `list`, `index` | Selects one element from an `Iterable` object and emits it into the frame. |

`name`, `list`, `item`, `index`, and `source` refer to context keys or frame keys. Context numbers and objects use expression-style names such as `target` or `baseDamage`. Frame outputs may also use dotted paths such as `damage.final`.

### Action JSON Formats

#### `pidatagraph:sequence`

```json
{
  "type": "pidatagraph:sequence",
  "children": [
    {
      "type": "pidatagraph:noop"
    }
  ]
}
```

#### `pidatagraph:noop`

```json
{
  "type": "pidatagraph:noop"
}
```

#### `pidatagraph:fail`

```json
{
  "type": "pidatagraph:fail",
  "message": "missing required target"
}
```

#### `pidatagraph:if`

```json
{
  "type": "pidatagraph:if",
  "condition": "resource >= cost",
  "then": {
    "type": "pidatagraph:emit_flag",
    "name": "accepted",
    "predicate": "1"
  },
  "else": {
    "type": "pidatagraph:noop"
  }
}
```

#### `pidatagraph:repeat`

```json
{
  "type": "pidatagraph:repeat",
  "times": "3",
  "index": "loopIndex",
  "child": {
    "type": "pidatagraph:noop"
  }
}
```

#### `pidatagraph:with_number`

```json
{
  "type": "pidatagraph:with_number",
  "name": "finalDamage",
  "value": "base + power * 2",
  "child": {
    "type": "pidatagraph:noop"
  }
}
```

#### `pidatagraph:with_context`

```json
{
  "type": "pidatagraph:with_context",
  "numbers": {
    "finalDamage": "base + power * 2"
  },
  "objects": {
    "target": "hitEntity"
  },
  "child": {
    "type": "pidatagraph:noop"
  }
}
```

#### `pidatagraph:guard`

```json
{
  "type": "pidatagraph:guard",
  "predicate": "resource >= cost",
  "child": {
    "type": "pidatagraph:noop"
  }
}
```

#### `pidatagraph:for_each_object`

```json
{
  "type": "pidatagraph:for_each_object",
  "list": "targets",
  "item": "target",
  "index": "targetIndex",
  "child": {
    "type": "pidatagraph:noop"
  }
}
```

#### `pidatagraph:emit_number`

```json
{
  "type": "pidatagraph:emit_number",
  "name": "damage.final",
  "value": "base + power * 2"
}
```

#### `pidatagraph:emit_random_number`

```json
{
  "type": "pidatagraph:emit_random_number",
  "name": "reward.roll",
  "min": "minReward",
  "max": "maxReward"
}
```

#### `pidatagraph:emit_flag`

```json
{
  "type": "pidatagraph:emit_flag",
  "name": "accepted",
  "predicate": "base > 0 & power > 0"
}
```

#### `pidatagraph:emit_object`

```json
{
  "type": "pidatagraph:emit_object",
  "name": "target.selected",
  "source": "target"
}
```

#### `pidatagraph:select_object`

```json
{
  "type": "pidatagraph:select_object",
  "name": "reward.selected",
  "list": "rewardPool",
  "index": "selectedIndex"
}
```

### Complete Action Chain Example

```json
{
  "type": "pidatagraph:with_context",
  "numbers": {
    "finalDamage": "baseDamage + enchantmentLevel * 0.5",
    "resourceAfter": "resource - cost"
  },
  "objects": {
    "target": "hitEntity"
  },
  "child": {
    "type": "pidatagraph:guard",
    "predicate": {
      "type": "pidatagraph:all",
      "predicates": [
        "resourceAfter >= 0",
        {
          "type": "pidatagraph:number_range",
          "key": "enchantmentLevel",
          "min": "1"
        }
      ]
    },
    "child": {
      "type": "pidatagraph:sequence",
      "children": [
        {
          "type": "pidatagraph:emit_number",
          "name": "damage.final",
          "value": "finalDamage"
        },
        {
          "type": "pidatagraph:emit_object",
          "name": "target.selected",
          "source": "target"
        },
        {
          "type": "pidatagraph:emit_flag",
          "name": "accepted",
          "predicate": "finalDamage > 0"
        }
      ]
    }
  }
}
```

## Core Predicates

| Type | JSON | Meaning |
| --- | --- | --- |
| Expression | `"mana >= cost"` | Boolean expression shortcut. |
| `pidatagraph:all` | `predicates` | Passes when every child passes. |
| `pidatagraph:any` | `predicates` | Passes when at least one child passes. |
| `pidatagraph:not` | `predicate` | Inverts one child predicate. |
| `pidatagraph:chance` | `chance` | Passes when context random is below the evaluated chance, clamped to `0..1`. |
| `pidatagraph:has_object` | `key` | Checks that a context object exists. |
| `pidatagraph:has_number` | `key` | Checks that a context number exists. |
| `pidatagraph:number_range` | `key`, optional `min`, optional `max` | Checks a number against expression bounds. |
| `pidatagraph:object_equals` | `left`, `right`, optional `identity` | Compares two context objects; `identity=true` requires the same instance. |
| `pidatagraph:entity_type` | `entity`, optional `entity_type`, optional `tag` | Checks an `Entity` context object against an entity type or entity type tag. |
| `pidatagraph:level_dimension` | `level`, `dimension` | Checks the dimension of a `Level` context object. |
| `pidatagraph:block_state` | `level`, `pos`, optional `block`, optional `tag` | Checks the block or block tag at a position. |
| `pidatagraph:biome` | `level`, `pos`, optional `biome`, optional `tag` | Checks the biome or biome tag at a position. |
| `pidatagraph:item_enchantment` | `stack`, `enchantment`, optional `min`, optional `max` | Checks an enchantment level on an `ItemStack`. |
| `pidatagraph:item_stack` | `stack`, optional item/count/damage fields | Checks an `ItemStack` context object. |

`item_stack` supports these optional fields: `item`, `tag`, `empty`, `min_count`, `max_count`, `min_damage`, `max_damage`, `max_damage_ratio`.

### Predicate JSON Formats

#### Expression shortcut

```json
"resource >= cost"
```

#### `pidatagraph:all`

```json
{
  "type": "pidatagraph:all",
  "predicates": [
    "resource >= cost",
    {
      "type": "pidatagraph:has_object",
      "key": "target"
    }
  ]
}
```

#### `pidatagraph:any`

```json
{
  "type": "pidatagraph:any",
  "predicates": [
    "resource >= cost",
    {
      "type": "pidatagraph:chance",
      "chance": "0.25"
    }
  ]
}
```

#### `pidatagraph:not`

```json
{
  "type": "pidatagraph:not",
  "predicate": {
    "type": "pidatagraph:has_number",
    "key": "cooldown"
  }
}
```

#### `pidatagraph:chance`

```json
{
  "type": "pidatagraph:chance",
  "chance": "0.25"
}
```

#### `pidatagraph:has_object`

```json
{
  "type": "pidatagraph:has_object",
  "key": "target"
}
```

#### `pidatagraph:has_number`

```json
{
  "type": "pidatagraph:has_number",
  "key": "resource"
}
```

#### `pidatagraph:number_range`

```json
{
  "type": "pidatagraph:number_range",
  "key": "distance",
  "min": "0",
  "max": "range"
}
```

#### `pidatagraph:object_equals`

```json
{
  "type": "pidatagraph:object_equals",
  "left": "target",
  "right": "owner",
  "identity": true
}
```

#### `pidatagraph:entity_type`

```json
{
  "type": "pidatagraph:entity_type",
  "entity": "target",
  "entity_type": "minecraft:zombie"
}
```

#### `pidatagraph:level_dimension`

```json
{
  "type": "pidatagraph:level_dimension",
  "level": "level",
  "dimension": "minecraft:overworld"
}
```

#### `pidatagraph:block_state`

```json
{
  "type": "pidatagraph:block_state",
  "level": "level",
  "pos": "blockPos",
  "block": "minecraft:stone"
}
```

#### `pidatagraph:biome`

```json
{
  "type": "pidatagraph:biome",
  "level": "level",
  "pos": "blockPos",
  "biome": "minecraft:plains"
}
```

#### `pidatagraph:item_enchantment`

```json
{
  "type": "pidatagraph:item_enchantment",
  "stack": "weapon",
  "enchantment": "minecraft:sharpness",
  "min": "1",
  "max": "5"
}
```

#### `pidatagraph:item_stack`

```json
{
  "type": "pidatagraph:item_stack",
  "stack": "weapon",
  "item": "minecraft:diamond_sword",
  "min_count": "1",
  "max_damage_ratio": "0.75"
}
```

## Where Java Keys Come From

JSON still uses normal names such as `"base"`, `"target"`, and `"damage.final"`. Gameplay code does not need to guess those names: `@PiGraphInput(..., facade = "HitGraph")` turns the input record into `HitGraph.Context`, and `@PiGraphOutput` turns the output record into `HitGraph.Output`.

```java
@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(double base, double power, LivingEntity target) {
}

@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(@PiOutput("damage.final") double damage) {
}
```

After compilation, Java code uses typed keys:

```java
PiEngineContextContract.builder()
        .number(HitGraph.Context.BASE)
        .number(HitGraph.Context.POWER)
        .object(HitGraph.Context.TARGET)
        .build();

PiEngineFrame.builder()
        .number(HitGraph.Output.DAMAGE_FINAL, damage)
        .build();
```

Datapacks and generated JSON still keep the normal Minecraft data shape:

```json
{
  "type": "pidatagraph:number_range",
  "key": "power",
  "min": "1"
}
```

JSON still stores plain names because datapacks need stable text keys. Java can use generated typed keys so action implementations, tests, and datagen do not repeat those names. Gameplay code should call `HitGraph.run(...)` or project gameplay methods instead of touching internal `_PiDataGraph` glue.

Typed object keys also carry their Java type into contract and reload/datagen validation. For example, `PiEmitObjectAction(outputKey, targetKey)` requires `targetKey.type()` in the context, and `PiForEachObjectAction(listKey, itemKey, child)` uses `itemKey.type()` when verifying the child action and rejects wrong list element types at runtime.

## Custom Actions

A custom action implements `PiEngineAction` and declares a `PiEngineActionType`. The Codec decodes JSON into the action object.

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
                .object(HitGraph.Context.TARGET)
                .object(HitGraph.Context.DAMAGE_SOURCE)
                .build();
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        LivingEntity target = context.requireObject(HitGraph.Context.TARGET);
        DamageSource source = context.requireObject(HitGraph.Context.DAMAGE_SOURCE);
        double damage = context.evaluate(amount);
        target.hurt(source, (float) damage);
        return PiEngineFrame.builder()
                .number(HitGraph.Output.DAMAGE_FINAL, damage)
                .object(HitGraph.Output.LAST_TARGET, target)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verify(path + ".amount", context, amount);
    }
}
```

Add the custom type to the registry, then JSON can use it:

```java
public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.builder()
        .install(PiEngineActions.core())
        .installPredicates(PiEnginePredicates.core())
        .add(DamageAction.TYPE)
        .build();
```

```json
{
  "type": "examplemod:damage",
  "amount": "base + power * 2"
}
```

`contextContract()` declares the runtime inputs. `verify(...)` declares expressions or child actions that should be checked during reload or datagen. Bad JSON then reports the entry id and field path before gameplay execution.
