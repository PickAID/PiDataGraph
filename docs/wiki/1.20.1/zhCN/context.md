# 执行上下文

`PiEngineContext` 是一次执行的输入。它有两类值：

- number：表达式、`number_range`、公式会读取它。
- object：Java 叶子 action 和对象类 predicate 会读取它。

Java 侧建议把稳定 key 收成常量。number 用 `PiEngineNumberKey`，object 用 `PiEngineContextKey<T>`，frame 里的 boolean 结果用 `PiEngineFlagKey`。`PiEngineFrame` 也能复用这些 key 读取输出：

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

JSON 里仍然写 `"baseDamage"`、`"resource"` 这些变量名；key 常量解决的是 Java 侧拼错和重复手写的问题。

读取结果时也可以复用同一组 key：

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

## key 从哪里来

- Java 调用方通过 `PiEngineContext.builder()` 放入。
- `repeat` / `for_each_object` 写入循环下标。
- `with_number` / `with_context` 派生临时 number。
- `with_context` 给已有 object 取别名后传给子链。

`number_range` 只检查一个已经存在的 number key：

```json
{
  "type": "pidatagraph:number_range",
  "key": "distance",
  "max": "range"
}
```

## 常用 key 约定

| 场景 | number | object |
| --- | --- | --- |
| 攻击或命中 | `baseDamage`, `distance`, `resource`, `cost`, `cooldown`, `enchantmentLevel` | `actor`, `target`, `weapon`, `damageSource` |
| 投掷物命中 | `baseDamage`, `speed`, `distance`, `age` | `projectile`, `owner`, `hitEntity`, `hitPos` |
| 机器 tick | `progress`, `energy`, `maxEnergy`, `temperature`, `speed` | `level`, `blockEntity`, `itemInput`, `itemOutput` |
| UI 预览 | `progress`, `resource`, `maxResource`, `cooldown` | `viewer`, `stack`, `previewTarget` |

## 快速绑定 Minecraft 对象

`PiEngineContextBindings` 把常见对象同时放成 object 和表达式可读的 number。

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

上面会提供：

| key | 值 |
| --- | --- |
| `impact` | `Vec3` object |
| `impactX`, `impactY`, `impactZ`, `impactLength` | 命中位置坐标和长度 |
| `weapon` | `ItemStack` object |
| `weaponCount`, `weaponDamage`, `weaponMaxDamage`, `weaponDamageRatio`, `weaponEmpty` | 物品堆数量和耐久状态 |

也可以绑定 `BlockPos`、`RandomSource`、`Entity`、`LivingEntity`、`Level`、`DamageSource`、`InteractionHand`。

## Context 契约

叶子 action 应该声明自己需要哪些运行时输入：

```java
PiEngineContextContract contract = action.contextContract();
```

校验时把入口会提供的 number 和 object 写清楚：

```java
action.verify(PiDataBuildContext.builder()
        .expressionScope(PiExpressionScope.of("baseDamage", "power", "resource", "cost"))
        .object("hitEntity", LivingEntity.class)
        .object("damageSource", DamageSource.class)
        .object("weapon", ItemStack.class)
        .build(), "examplemod:fire_hit");
```

如果你已经有 action 契约，可以让 build context 吸收它：

```java
PiEngineBuildContext buildContext = PiEngineBuildContext.standard()
        .withScope(PiExpressionScope.of("baseDamage", "power", "resource", "cost"))
        .withContextContract(action.contextContract());
```
