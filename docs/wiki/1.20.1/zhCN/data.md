# 数据文件

PiDataGraph 的主线用法是：用注解声明一类 datapack action 数据，编译器生成可直接调用的 facade。普通玩法代码、datagen 和 action 实现都应该依赖这个 facade，而不是自己再包一层 `_PiDataGraph`。

如果给 `@PiGraphInput` 写了 `facade = "HitGraph"`，编译后会生成 `HitGraph`。`ExampleGraphs_PiDataGraph` 仍然会存在，但它是生成器内部 glue，不是应用层入口。

## 最终 JSON

下面这个文件由 `@PiDataPackRegistry(folder = "skills/hit")` 决定路径：

```text
data/examplemod/skills/hit/fire_hit.json
```

文件内容是一条 action chain：

```json
{
  "type": "pidatagraph:sequence",
  "children": [
    {
      "type": "pidatagraph:emit_number",
      "name": "damage.final",
      "value": "base + power * 2"
    },
    {
      "type": "pidatagraph:emit_flag",
      "name": "accepted",
      "predicate": "base > 0 & power > 0"
    },
    {
      "type": "pidatagraph:emit_object",
      "name": "lastTarget",
      "source": "target"
    }
  ]
}
```

JSON 只描述数据：action type、表达式、输入 key 和输出 key。Java 类负责提供输入对象、注册 action type、执行并读取输出。

## 声明 Registry

```java
@PiDataGraphModule(modid = "examplemod")
public final class ExampleGraphs {
    @PiDataPackRegistry(
            path = "hit_action",
            folder = "skills/hit",
            sync = PiDataPackSync.SYNC_TO_CLIENT)
    public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.builder()
            .install(PiEngineActions.core())
            .installPredicates(PiEnginePredicates.core())
            .build();

    private ExampleGraphs() {
    }
}
```

`path = "hit_action"` 是 datapack registry id。`folder = "skills/hit"` 是 JSON 文件夹。不写 `folder` 时默认用 `path`。

## 声明输入和输出

`HitInput` 是运行一次 action 时要提供的值。`facade = "HitGraph"` 会生成稳定入口类。

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

`HitOutput` 是 action 执行完后要读回的值。

```java
@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(
        @PiOutput("damage.final") double damage,
        boolean accepted,
        LivingEntity lastTarget
) {
}
```

## 自动接入

`@PiDataPackRegistry` 会让 processor 生成 Forge subscriber。正常项目不需要在 mod 构造器里写 `modBus.addListener(...)`。

生成代码会自动做两件事：

- 在 MOD bus 的 `DataPackRegistryEvent.NewRegistry` 阶段注册 datapack registry。
- 在 FORGE bus 的 reload 阶段添加 verifier，reload 后检查每个 action 的输入需求和表达式。

`HitGraph.registerDatapackRegistry(event)` 和 `HitGraph.addReloadVerifier(event)` 仍然会生成，但它们是低层入口。只有测试、特殊加载流程或你明确不想用自动 subscriber 时才需要直接调用。

## `HitGraph` Facade 方法列表

### ID 函数

| 函数 | 用途 |
| --- | --- |
| `HitGraph.id("fire_hit")` | 得到完整资源 id：`examplemod:fire_hit`。适合 datagen、日志、配置读取、普通查找。 |
| `HitGraph.key("fire_hit")` | 得到 datapack registry entry key。适合需要 `ResourceKey<?>` 的 registry API 或同步 API。 |

### Datagen 函数

| 函数 | 用途 |
| --- | --- |
| `HitGraph.dataSet()` | 创建当前 mod namespace 下的数据集 builder。给自己模组生成默认 JSON 时用。 |
| `HitGraph.dataSet("other_namespace")` | 创建指定 namespace 下的数据集 builder。给兼容包、测试数据或其他 namespace 写 JSON 时用。 |
| `HitGraph.validationContext()` | 创建验证上下文。传给 `PiDataProvider` 后，写文件前会检查表达式、输入 key、action contract 和输出 key。 |

### 运行函数

| 函数 | 用途 |
| --- | --- |
| `HitGraph.run(level.registryAccess(), "fire_hit", input)` | 正常服务器玩法逻辑使用。从当前 datapack registry 读取并执行。 |
| `HitGraph.run(level.registryAccess(), id, input)` | 已经有 `ResourceLocation` 时使用，例如 id 来自配置、别的 registry 或网络同步。 |
| `HitGraph.run(level.registryAccess(), key, input)` | 已经有 `ResourceKey<?>` 时使用，例如和其他 registry API 串接。 |
| `HitGraph.run(dataSet, "fire_hit", input)` | 测试、datagen 工具、无世界环境使用。从临时数据集执行。 |
| `HitGraph.run(dataSet, id, input)` | 临时数据集执行，并且调用方已经有完整 `ResourceLocation`。 |
| `HitGraph.run(dataSet, key, input)` | 临时数据集执行，并且调用方已经有 registry entry key。 |

### 事件函数

| 函数 | 用途 |
| --- | --- |
| `HitGraph.registerDatapackRegistry(event)` | 手动注册 datapack registry。正常项目由生成的 Forge MOD bus subscriber 自动调用。 |
| `HitGraph.addReloadVerifier(event)` | 手动挂 reload 校验。正常项目由生成的 Forge FORGE bus subscriber 自动调用。 |

正常项目只需要声明注解和使用 `HitGraph.run(...)`、`HitGraph.dataSet(...)` 这类业务入口。事件函数保留给测试和特殊加载流程，不是日常样板代码。

## 自定义 Action

Action 使用 facade 暴露的 key。输入 key 在 `HitGraph.Context` 里，输出 key 在 `HitGraph.Output` 里。

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
                .number(HitGraph.Context.POWER)
                .object(HitGraph.Context.TARGET)
                .build();
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        double damage = context.number(HitGraph.Context.BASE)
                + context.number(HitGraph.Context.POWER) * 2;
        return PiEngineFrame.builder()
                .number(HitGraph.Output.DAMAGE_FINAL, damage)
                .flag(HitGraph.Output.ACCEPTED, true)
                .object(HitGraph.Output.LAST_TARGET, context.requireObject(HitGraph.Context.TARGET))
                .build();
    }
}
```

## 生成默认数据

Datagen 也只用 `HitGraph`：

```java
PiDataSet<PiEngineAction> defaults = HitGraph.dataSet()
        .entry("fire_hit",
                new PiSequenceAction(List.of(
                        new PiEmitNumberAction(
                                HitGraph.Output.DAMAGE_FINAL,
                                PiDoubleExpression.of("base + power * 2")),
                        new PiEmitFlagAction(
                                HitGraph.Output.ACCEPTED,
                                PiExpressionPredicate.of("base > 0 & power > 0")),
                        new PiEmitObjectAction(
                                HitGraph.Output.LAST_TARGET,
                                HitGraph.Context.TARGET))))
        .build();

gen.addProvider(server, new PiDataProvider(
        output,
        "ExampleMod Hit Action Data",
        HitGraph.validationContext(),
        defaults));
```

`PiDataProvider` 的第二个参数是 datagen 显示名，只用于日志和任务展示；它不决定 registry id、文件路径或 namespace。`PiDataProvider` 写文件前会校验数据。如果表达式引用不存在的 number，例如 `missingPower`，datagen 会失败。

## 运行

```java
HitInput input = new HitInput(base, power, target, source);
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);

if (output.accepted()) {
    target.hurt(source, (float) output.damage());
}
```

`fire_hit` 是 datapack entry id，不是 Java 方法名。处理器不会根据数据包里的每个文件生成 `fireHit(...)` 这种业务方法，因为数据包条目可以由玩家或其他包新增、删除、改名。稳定入口是 `HitGraph.run(..., "fire_hit", input)`。

如果某个项目想把调用再缩短，可以在自己的玩法类里写业务方法，但这不是 PiDataGraph 必须要求的样板代码。

## 可以改什么

数据包可以改：

- `value` 公式，例如 `"base + power * 3"`。
- `predicate` 条件，例如 `"base > 0 & power >= 2"`。
- action chain 结构，例如加 `pidatagraph:guard`、`pidatagraph:if`、`pidatagraph:repeat`。
- 输出 key，只要 Java 的 output record 仍然读取这些 key。

不要随便改：

- 输入 key，例如 `base`、`target`，除非 Java input record 也一起改。
- 自定义 action type，除非 Java registry 已经注册。
- output record 需要的 key，否则读取输出时会报错。

## 表达式语法

| 类型 | 支持内容 |
| --- | --- |
| 数学 | `+`, `-`, `*`, `/`, `%`, `^` |
| 比较 | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| 逻辑 | `&`, `|` |

### 表达式函数列表

这些函数写在表达式字符串里，例如 `"clamp(base + power * 2, 0, 40)"`。

| 函数 | 参数 | 用途 |
| --- | --- | --- |
| `min(a, b)` | 两个数字 | 返回较小值。 |
| `max(a, b)` | 两个数字 | 返回较大值。 |
| `clamp(value, min, max)` | 值、下限、上限 | 把 `value` 限制在 `[min, max]` 之间。 |
| `abs(value)` | 一个数字 | 返回绝对值。 |
| `floor(value)` | 一个数字 | 向下取整。`floor(2.9)` 是 `2`。 |
| `ceil(value)` | 一个数字 | 向上取整。`ceil(2.1)` 是 `3`。 |
| `round(value)` | 一个数字 | 取最接近的整数，行为跟 Java `Math.rint` 一致。 |
| `sqrt(value)` | 一个数字 | 平方根。 |
| `pow(base, exponent)` | 底数、指数 | 幂运算。`pow(2, 3)` 是 `8`。 |
| `sin(value)` | 弧度 | 正弦。注意参数是弧度，不是角度。 |
| `cos(value)` | 弧度 | 余弦。注意参数是弧度，不是角度。 |
| `tan(value)` | 弧度 | 正切。注意参数是弧度，不是角度。 |
| `rand(min, max)` | 下限、上限 | 返回 `[min, max)` 范围内的随机数，随机源来自当前 `PiEngineContext`。 |

boolean 表达式里，结果大于 `0.5` 视为 true。

### 自定义表达式函数

默认生成 facade 使用标准表达式语言，也就是上表这些函数。低层 runner、自定义 binder 或手动校验如果需要额外函数，可以用 `PiExpressionLanguage.standardBuilder()` 扩展，并确保 datagen/reload 校验和运行时使用同一个 language。

```java
PiExpressionLanguage language = PiExpressionLanguage.standardBuilder()
        .function("damage_scale", 2, (context, args) ->
                args[0] * context.variables().get("power") + args[1])
        .build();

PiDataBuildContext buildContext = PiDataBuildContext.builder()
        .expressionLanguage(language)
        .number(HitGraph.Context.POWER)
        .build();

PiEngineContext runtimeContext = PiEngineContext.builder()
        .language(language)
        .number(HitGraph.Context.POWER, 4)
        .build();
```

如果只在运行时注册函数，而 datagen/reload 校验没有使用同一个 language，数据生成或 reload 会失败。这是有意设计，目的是让错误尽早暴露。

## 低层 JSON API

`PiDataDefinition`、`PiDataSet`、`PiDataProvider`、`PiDataReloadListener` 仍然保留。它们适合纯 JSON 表，不适合作为复杂玩法逻辑的推荐入口。需要 action、runner、input/output record、datapack registry 和 reload 校验时，优先使用本页的注解系统。
