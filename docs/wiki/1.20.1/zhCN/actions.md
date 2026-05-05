# 动作链

Action chain 用来把“可配置的流程”放进数据包：先派生临时数值，再判断条件，然后执行一个或多个步骤。真正修改世界的部分仍然写在 Java 叶子 action 里，例如造成伤害、扣资源、移动目标、发粒子、写缓存。

## 一个完整的动作文件

假设文件路径是：

```text
data/examplemod/spell_actions/fire_hit.json
```

内容可以这样写：

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

这段 JSON 做了四件事：

- `with_context` 计算 `finalDamage` 和 `manaAfterCast`，并把 `hitEntity` 这个 object 起别名为 `target`。
- `guard` 只在资源足够且伤害大于 0 时继续执行。
- `examplemod:damage` 是项目自己注册的 Java action，负责真正调用 Minecraft API。
- `emit_number` 和 `emit_flag` 把执行结果写进 `PiEngineFrame`，方便调用方更新 UI、日志或后续逻辑。

## 执行一个动作

运行时先从库里取出 action，然后提供本次执行需要的 context。

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

`PiEngineContext` 是输入，`PiEngineFrame` 是输出。输入来自当前事件、实体、物品、方块实体、UI 预览或其他运行时对象；输出由 action chain 自己选择写入。
上面的示例使用 `guard`，所以条件不通过时会返回空 frame；调用方读取 frame 前应该先判断 key 是否存在。

## 内置 action

| type | 必填字段 | 可选字段 | 作用 |
| --- | --- | --- | --- |
| `pidatagraph:sequence` | `children` |  | 顺序执行多个子 action，并合并它们的 frame。 |
| `pidatagraph:guard` | `predicate`, `child` |  | predicate 通过才执行 child。 |
| `pidatagraph:if` | `condition` | `then`, `else` | 条件分支。缺少分支时返回空 frame。 |
| `pidatagraph:repeat` | `times`, `child` | `index` | 重复执行 child。`times` 小于 0 时按 0 次处理。 |
| `pidatagraph:for_each_object` | `list`, `item`, `child` | `index` | 遍历 context 中的 `Iterable` object，把当前元素写成 `item`。 |
| `pidatagraph:with_number` | `name`, `value`, `child` |  | 给 child 派生一个临时 number。 |
| `pidatagraph:with_context` | `child` | `numbers`, `objects` | 一次派生多个 number，或把已有 object 改名传给 child。 |
| `pidatagraph:emit_number` | `name`, `value` |  | 把表达式结果写入 frame。 |
| `pidatagraph:emit_flag` | `name`, `predicate` |  | 把 predicate 结果写入 frame。 |
| `pidatagraph:emit_object` | `name`, `source` |  | 把 context 中的 object 写入 frame。 |

`sequence` 合并 frame 时不允许重复 key。需要覆盖旧值时，不要用两个 `emit_*` 写同一个名字；把覆盖语义写进项目自己的 Java action 更清楚。

## 内置 predicate

predicate 可以直接写成字符串：

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

| type | 字段 | 作用 |
| --- | --- | --- |
| 字符串 |  | 布尔表达式，返回值大于 `0.5` 视为 true。 |
| `pidatagraph:all` | `predicates` | 所有子 predicate 都通过。 |
| `pidatagraph:any` | `predicates` | 任一子 predicate 通过。 |
| `pidatagraph:not` | `predicate` | 对结果取反。 |
| `pidatagraph:chance` | `chance` | 概率判断，`chance` 是表达式。 |
| `pidatagraph:has_number` | `key` | 检查 context 是否有 number。 |
| `pidatagraph:has_object` | `key` | 检查 context 是否有 object。 |
| `pidatagraph:number_range` | `key`, `min`, `max` | 检查 number 范围。 |
| `pidatagraph:item_enchantment` | `stack`, `enchantment`, `min`, `max` | 检查 `ItemStack` 上的附魔等级。 |
| `pidatagraph:item_stack` | `stack`, `item`, `tag`, `empty`, `min_count`, `max_count`, `min_damage`, `max_damage`, `max_damage_ratio` | 检查 `ItemStack` 的物品、标签、数量和耐久。 |

物品相关 predicate 需要 context 里有 `ItemStack` object。可以手动放入，也可以用 `PiEngineContextBindings.itemStack(...)` 生成 object 和常用 number。

## 注册 Java 叶子 action

项目 action 负责连接 Minecraft API。下面这个 action 从 context 里取 `target` 和 `damageSource`，再用 JSON 中的 `amount` 表达式计算伤害。

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

注册 action registry：

```java
public static final PiEngineActionRegistry SPELL_ACTIONS = PiEngineActionRegistry.builder()
        .install(PiEngineActions.core())
        .installPredicates(PiEnginePredicates.core())
        .add(DamageAction.TYPE)
        .build();
```

如果自定义 action 内部还包含 child action，使用 `PiEngineActionType.of(id, (actionCodec, predicateCodec) -> ...)`，这样 Codec 可以递归读取子 action。

## 把 action 放进数据包

`PiEngineActionData` 提供了标准 definition。`folder` 决定 JSON 路径。

```java
public static final PiDataDefinition<PiEngineAction> SPELL_ACTION_DATA =
        PiEngineActionData.definition(
                new ResourceLocation("examplemod", "spell_actions"),
                "spell_actions",
                SPELL_ACTIONS);
```

上面的 `folder = "spell_actions"` 会读取：

```text
data/<namespace>/spell_actions/<path>.json
```

加载后可以直接编译成 runtime library：

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

如果表达式写错、object 名字缺失、类型不匹配，加载阶段会直接报出路径，例如：

```text
examplemod:fire_hit/root.child.children.0.amount: invalid expression `finalDamage + missing`
```

## 常见规则

- key 名字必须能作为表达式变量使用，例如 `finalDamage`、`mana_after`；不要用 `spell.damage` 这种带点号的名字作为 context key。
- `with_context.objects` 的方向是“新名字 -> 旧名字”。`"target": "hitEntity"` 表示 child 里读取 `target`，实际对象来自父 context 的 `hitEntity`。
- `for_each_object.list` 必须是 `Iterable`，比如 `List<LivingEntity>`。
- `emit_object.source` 只复制已有 object，不会创建新对象。
- Java 叶子 action 应该写 `contextContract()`，否则数据加载阶段无法提前发现缺少 object 的问题。
- 数据包负责组合流程，Java 负责执行 Minecraft API。不要把会修改世界的逻辑藏进 expression 函数里。
