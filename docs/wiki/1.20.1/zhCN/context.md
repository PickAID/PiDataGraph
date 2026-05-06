# 执行上下文

`PiEngineContext` 是一次 action 执行时的输入环境。number 会进入表达式变量表，object 会交给 Java 侧 action 使用。

普通玩法代码通常不需要手动创建 `PiEngineContext`。如果使用 `@PiGraphInput(..., facade = "HitGraph")`，生成的 `HitGraph.run(...)` 会把 `HitInput` 自动绑定成 context。

## 生成 key

输入 record：

```java
@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(
        double base,
        double power,
        LivingEntity target,
        @PiObject("damageSource") DamageSource source
) {
}
```

输出 record：

```java
@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(
        @PiOutput("damage.final") double damage,
        boolean accepted,
        LivingEntity lastTarget
) {
}
```

这会生成两组 key：

```java
HitGraph.Context.BASE;
HitGraph.Context.POWER;
HitGraph.Context.TARGET;
HitGraph.Context.DAMAGE_SOURCE;

HitGraph.Output.DAMAGE_FINAL;
HitGraph.Output.ACCEPTED;
HitGraph.Output.LAST_TARGET;
```

不要为了这些 key 手写 `HitGraph.java`。它们由 annotation processor 生成，action、datagen 和低层工具直接引用生成类即可。

## Action 里读取输入

自定义 action 从 `PiEngineContext` 里读取输入，再把结果写入 `PiEngineFrame`。

```java
@Override
public PiEngineFrame execute(PiEngineContext context) {
    double damage = context.number(HitGraph.Context.BASE)
            + context.number(HitGraph.Context.POWER) * 2;
    LivingEntity target = context.requireObject(HitGraph.Context.TARGET);

    return PiEngineFrame.builder()
            .number(HitGraph.Output.DAMAGE_FINAL, damage)
            .flag(HitGraph.Output.ACCEPTED, true)
            .object(HitGraph.Output.LAST_TARGET, target)
            .build();
}
```

规则：

- context number 必须是表达式变量名，例如 `base`、`power`。
- context object 也使用普通变量名，例如 `target`、`damageSource`。
- frame 输出可以用普通变量名，也可以用 dotted path，例如 `damage.final`。

## 玩法代码读取输出

如果使用生成 facade，玩法代码直接拿 output record，不需要直接操作 `PiEngineFrame`。

```java
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);

if (output.accepted()) {
    target.hurt(source, (float) output.damage());
}
```

低层 runner 或公式组才需要直接读取 frame：

```java
PiEngineFrame frame = runner.run(dataSet, HitGraph.id("fire_hit"), input);

double damage = frame.numberOr(HitGraph.Output.DAMAGE_FINAL, 0);
boolean accepted = frame.flagOr(HitGraph.Output.ACCEPTED, false);
LivingEntity lastTarget = frame.value(HitGraph.Output.LAST_TARGET);
```

## Minecraft 绑定 helper

`PiEngineContextBindings` 用于低层 runner、测试或项目自定义 binder。它会把 Minecraft 对象拆成 object 和常用 number。

```java
PiEngineContext context = PiEngineContextBindings.random(
        PiEngineContextBindings.living(
                PiEngineContext.builder(),
                HitGraph.Context.TARGET,
                target),
        level.random)
        .number(HitGraph.Context.BASE, 6)
        .number(HitGraph.Context.POWER, 3)
        .object(HitGraph.Context.DAMAGE_SOURCE, source)
        .build();
```

上面会写入 `target` object，也会额外写入 `targetX`、`targetY`、`targetHealth`、`targetArmor` 等常用 number。表达式可以使用这些 number，但 datagen/reload 校验也必须知道它们存在。

注意：这些额外 number 只存在于你手动使用 `PiEngineContextBindings` 构造 context 的低层路径。普通 `HitGraph.run(level.registryAccess(), "fire_hit", input)` 只会绑定 `HitInput` record 里声明的 number/object。如果数据包表达式要直接使用 `targetHealth`，要么把它作为 `HitInput` 的 number 字段传入，要么使用项目自己的 binder/runner 路径。

已支持的绑定：

| 方法 | 写入内容 |
| --- | --- |
| `level` | `level`, `gameTime`, `dayTime`, `clientSide` |
| `random` | `random` object，并接入表达式 `rand(min, max)` |
| `entity` | entity object、坐标、旋转、tick、onGround、delta |
| `living` | entity 内容，加 health、maxHealth、absorption、armor |
| `vector` | `Vec3` object，加 X/Y/Z/Length |
| `blockPos` | `BlockPos` object，加 X/Y/Z |
| `itemStack` | `ItemStack` object，加 count、damage、maxDamage、damageRatio、empty |
| `damageSource` | `damageSource` object |
| `hand` | `hand` object |

## Contract

自定义 action 用 `PiEngineContextContract` 声明自己需要哪些输入。runner、reload verifier 和 datagen verifier 会用它提前发现缺失 key。

```java
@Override
public PiEngineContextContract contextContract() {
    return PiEngineContextContract.builder()
            .number(HitGraph.Context.BASE)
            .number(HitGraph.Context.POWER)
            .object(HitGraph.Context.TARGET)
            .object(HitGraph.Context.DAMAGE_SOURCE)
            .build();
}
```

如果需要在低层校验里手动构造 build context，也可以继续使用同一批 key：

```java
PiDataBuildContext context = PiDataBuildContext.builder()
        .number(HitGraph.Context.BASE)
        .number(HitGraph.Context.POWER)
        .object(HitGraph.Context.TARGET)
        .object(HitGraph.Context.DAMAGE_SOURCE)
        .build();
```

移除或查询 object 需求时也可以传 typed key：

```java
PiEngineContextContract remaining = action.contextContract()
        .withoutObject(HitGraph.Context.TARGET);
```
