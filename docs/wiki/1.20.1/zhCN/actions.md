# 动作和条件

`PiEngineAction` 描述可配置流程。JSON 负责组合流程，Java action 负责真正调用 Minecraft API。

## 一个完整 action JSON

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

## 内置 action

| type | 字段 | 作用 |
| --- | --- | --- |
| `pidatagraph:sequence` | `children` | 顺序执行子 action，并合并 frame |
| `pidatagraph:guard` | `predicate`, `child` | 条件通过才执行 child |
| `pidatagraph:if` | `condition`, `then`, `else` | 分支执行 |
| `pidatagraph:repeat` | `times`, `child`, `index` | 重复执行，`index` 可选 |
| `pidatagraph:for_each_object` | `list`, `item`, `child`, `index` | 遍历 context 里的 `Iterable` object |
| `pidatagraph:with_number` | `name`, `value`, `child` | 给 child 派生一个 number |
| `pidatagraph:with_context` | `numbers`, `objects`, `child` | 同时派生多个 number，或给 object 取别名 |
| `pidatagraph:emit_number` | `name`, `value` | 写入 number frame 输出 |
| `pidatagraph:emit_flag` | `name`, `predicate` | 写入 boolean frame 输出 |
| `pidatagraph:emit_object` | `name`, `source` | 把 context object 复制到 frame |

`sequence` 不允许两个子 action 写同一个 frame key。

## 内置 predicate

predicate 可以直接写表达式字符串：

```json
"mana >= cost & cooldown == 0"
```

也可以写成带 `type` 的对象：

```json
{
  "type": "pidatagraph:number_range",
  "key": "distance",
  "max": "range"
}
```

| type | 字段 |
| --- | --- |
| string | boolean 表达式 |
| `pidatagraph:all` | `predicates` |
| `pidatagraph:any` | `predicates` |
| `pidatagraph:not` | `predicate` |
| `pidatagraph:chance` | `chance` |
| `pidatagraph:has_number` | `key` |
| `pidatagraph:has_object` | `key` |
| `pidatagraph:number_range` | `key`, `min`, `max` |
| `pidatagraph:item_enchantment` | `stack`, `enchantment`, `min`, `max` |
| `pidatagraph:item_stack` | `stack`, `item`, `tag`, `empty`, `min_count`, `max_count`, `min_damage`, `max_damage`, `max_damage_ratio` |

`item_stack` 和 `item_enchantment` 读取 context 里的 `ItemStack` object。

## 自定义 Java action

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

PiDataGraph action 只描述结构化请求。真正拥有运行时状态的模块，比如
PiEngine 加 PiDamage，会重新解析 target/source 状态并执行请求；graph action
不应该直接修改世界。

注册 action registry：

```java
public static final PiEngineActionRegistry SPELL_ACTIONS = PiEngineActionRegistry.builder()
        .install(PiEngineActions.core())
        .installPredicates(PiEnginePredicates.core())
        .add(DamageAction.TYPE)
        .build();
```

声明数据文件：

```java
public static final PiDataDefinition<PiEngineAction> SPELL_ACTION_DATA =
        PiEngineActionData.definition(
                new ResourceLocation("examplemod", "spell_actions"),
                "spell_actions",
                SPELL_ACTIONS);
```

读取路径：

```text
data/<namespace>/spell_actions/<path>.json
```

运行时读取 frame：

```java
PiEngineValueKey<Number> DAMAGE = PiEngineValueKey.number("result.damage");
PiEngineValueKey<Boolean> ACCEPTED = PiEngineValueKey.flag("result.accepted");

PiEngineFrame frame = action.execute(context);
double damage = frame.numberOr(DAMAGE, 0);
boolean accepted = frame.flagOr(ACCEPTED, false);
```
