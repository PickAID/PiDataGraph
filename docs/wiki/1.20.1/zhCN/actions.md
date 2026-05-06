# Action 和 Predicate

Action JSON 由 `PiEngineActionRegistry` 解码。可以直接用 `PiEngineActionRegistry.standard()`，也可以在自己的 registry 里安装 `PiEngineActions.core()` 和 `PiEnginePredicates.core()`。

每个 action object 都有 `type`。predicate 可以直接写 boolean 表达式字符串，也可以写带 `type` 的 object。

## 内置 Action

| Type                          | 字段                                    | 运行行为                                                 |
| ----------------------------- | --------------------------------------- | -------------------------------------------------------- |
| `pidatagraph:sequence`        | `children`                              | 按顺序执行 children，并合并 frame。重复输出 key 会失败。 |
| `pidatagraph:noop`            | 无                                      | 不做任何事，返回空 frame。适合占位或临时关闭分支。      |
| `pidatagraph:fail`            | `message`                               | 执行到这里时抛出带 message 的异常。                      |
| `pidatagraph:if`              | `condition`, 可选 `then`, 可选 `else`   | 执行一个分支。缺失分支会返回空 frame。                   |
| `pidatagraph:repeat`          | `times`, 可选 `index`, `child`          | 执行 `child` 最多 `times` 次；负数次数会变成 `0`。       |
| `pidatagraph:with_number`     | `name`, `value`, `child`                | 给 `child` 增加一个派生 number。                         |
| `pidatagraph:with_context`    | 可选 `numbers`, 可选 `objects`, `child` | 给 `child` 增加派生 number 和重映射 object。             |
| `pidatagraph:guard`           | `predicate`, `child`                    | predicate 通过时才执行 `child`。                         |
| `pidatagraph:for_each_object` | `list`, `item`, 可选 `index`, `child`   | 遍历一个 `Iterable` object，把每个元素绑定为 `item`。    |
| `pidatagraph:emit_number`     | `name`, `value`                         | 输出一个数字 frame value。                               |
| `pidatagraph:emit_random_number` | `name`, `min`, `max`                 | 使用当前 context 的随机源输出 `[min, max)` 数字。         |
| `pidatagraph:emit_flag`       | `name`, `predicate`                     | 输出一个 boolean frame value。                           |
| `pidatagraph:emit_object`     | `name`, `source`                        | 把 context object 复制到 frame。                         |
| `pidatagraph:select_object`   | `name`, `list`, `index`                 | 从 `Iterable` object 里按 index 取一个元素输出到 frame。 |

`name`、`list`、`item`、`index`、`source` 指向 context key 或 frame key。context number 和 object 使用表达式变量风格的名字，例如 `target` 或 `baseDamage`。frame 输出也可以用 dotted path，例如 `damage.final`。

### Action JSON 格式

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

### 完整 Action Chain 示例

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

## 内置 Predicate

| Type                           | JSON                                           | 含义                                                        |
| ------------------------------ | ---------------------------------------------- | ----------------------------------------------------------- |
| 表达式                         | `"mana >= cost"`                               | boolean 表达式快捷写法。                                    |
| `pidatagraph:all`              | `predicates`                                   | 所有 child 都通过时才通过。                                 |
| `pidatagraph:any`              | `predicates`                                   | 至少一个 child 通过时通过。                                 |
| `pidatagraph:not`              | `predicate`                                    | 反转一个 predicate。                                        |
| `pidatagraph:chance`           | `chance`                                       | context random 小于 chance 时通过；chance 会限制在 `0..1`。 |
| `pidatagraph:has_object`       | `key`                                          | 检查 context object 是否存在。                              |
| `pidatagraph:has_number`       | `key`                                          | 检查 context number 是否存在。                              |
| `pidatagraph:number_range`     | `key`, 可选 `min`, 可选 `max`                  | 检查 number 是否落在表达式边界内。                          |
| `pidatagraph:object_equals`    | `left`, `right`, 可选 `identity`               | 比较两个 context object；`identity=true` 时比较同一实例。   |
| `pidatagraph:entity_type`      | `entity`, 可选 `entity_type`, 可选 `tag`       | 检查 `Entity` context object 的实体类型或实体类型 tag。     |
| `pidatagraph:level_dimension`  | `level`, `dimension`                           | 检查 `Level` context object 所在维度。                      |
| `pidatagraph:block_state`      | `level`, `pos`, 可选 `block`, 可选 `tag`       | 检查某个位置的方块或方块 tag。                              |
| `pidatagraph:biome`            | `level`, `pos`, 可选 `biome`, 可选 `tag`       | 检查某个位置的 biome 或 biome tag。                         |
| `pidatagraph:item_enchantment` | `stack`, `enchantment`, 可选 `min`, 可选 `max` | 检查 `ItemStack` 上的附魔等级。                             |
| `pidatagraph:item_stack`       | `stack`, 可选 item/count/damage 字段           | 检查一个 `ItemStack` context object。                       |

`item_stack` 支持这些可选字段：`item`、`tag`、`empty`、`min_count`、`max_count`、`min_damage`、`max_damage`、`max_damage_ratio`。

### Predicate JSON 格式

#### 表达式快捷写法

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

## Java key 从哪里来

JSON 里仍然写普通名字，例如 `"base"`、`"target"`、`"damage.final"`。这些名字不是让调用代码手猜的：`@PiGraphInput(..., facade = "HitGraph")` 会把输入 record 变成 `HitGraph.Context`，`@PiGraphOutput` 会把输出 record 变成 `HitGraph.Output`。

```java
@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(double base, double power, LivingEntity target) {
}

@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(@PiOutput("damage.final") double damage) {
}
```

生成后，Java 代码使用 typed key：

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

数据包和生成出来的 JSON 仍然是 Minecraft 风格的数据格式：

```json
{
    "type": "pidatagraph:number_range",
    "key": "power",
    "min": "1"
}
```

JSON 仍然保存普通名字，因为数据包需要稳定的文本 key。Java 侧用生成的 typed key，可以让 action 实现、测试和 datagen 不重复写这些名字。普通玩法代码只调用 `HitGraph.run(...)` 或项目自己的业务方法，不直接接触内部 `_PiDataGraph` glue。

typed object key 也会把 Java 类型带进 contract 和 reload/datagen 校验。比如 `PiEmitObjectAction(outputKey, targetKey)` 会要求 context 里有 `targetKey.type()`，`PiForEachObjectAction(listKey, itemKey, child)` 会用 `itemKey.type()` 校验 child，并在运行时拒绝错误类型的 list 元素。

## 自定义 Action

自定义 action 需要实现 `PiEngineAction`，并声明一个 `PiEngineActionType`。Codec 负责把 JSON 解码成 action 对象。

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

把自定义 type 加进 registry 后，JSON 就可以使用它：

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

`contextContract()` 声明运行时必须提供的输入，`verify(...)` 声明数据包加载时要提前检查的表达式或子 action。这样 JSON 写错时会在 reload/datagen 阶段报出 entry id 和字段路径，而不是等到真正执行时才失败。
