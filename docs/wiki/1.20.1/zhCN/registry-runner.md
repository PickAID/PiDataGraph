# 数据包注册表和 Runner

PiDataGraph 可以把 action 放进 Minecraft datapack registry。运行时从 `RegistryAccess` 取数据，不需要自己维护 reload cache。

内置 action 和 predicate 的 JSON 结构见 [Action 和 Predicate](actions.md)。

## 推荐写法：生成 Facade

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
            .add(DamageAction.TYPE)
            .build();
}

@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(
        double base,
        double power,
        LivingEntity target,
        @PiObject("damageSource") DamageSource source) {
}

@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(@PiOutput("damage.final") double damage, boolean accepted, LivingEntity lastTarget) {
}
```

上面的声明会生成 `HitGraph`。这个类由 annotation processor 生成，不需要手写。第一次写完注解后，如果 IDEA 还没识别，先运行一次 `./gradlew compileJava` 或 Reload Gradle。

常用入口都在 `HitGraph` 上：

```java
HitGraph.id("fire_hit");
HitGraph.key("fire_hit");
HitGraph.dataSet();
HitGraph.dataSet("other_namespace");
HitGraph.validationContext();
HitGraph.run(level.registryAccess(), "fire_hit", input);
HitGraph.run(dataSet, "fire_hit", input);
```

### ID 函数

| 函数 | 什么时候用 |
| --- | --- |
| `id("fire_hit")` | 只需要资源 id 时用。结果是 `examplemod:fire_hit`。 |
| `key("fire_hit")` | 需要 datapack registry entry key 时用。同步、registry API、需要 `ResourceKey<?>` 的方法用它。 |

### Datagen 函数

| 函数 | 什么时候用 |
| --- | --- |
| `dataSet()` | 给当前 mod 生成默认 JSON 时用。 |
| `dataSet("other_namespace")` | 给其他 namespace 生成 JSON 时用。 |
| `validationContext()` | datagen 或手动验证时用，告诉验证器有哪些输入 number/object。 |

### 运行函数

| 函数 | 什么时候用 |
| --- | --- |
| `run(level.registryAccess(), "fire_hit", input)` | 服务器运行时从当前数据包 registry 读取并执行。 |
| `run(dataSet, "fire_hit", input)` | 测试、datagen 工具或无世界环境下，用临时数据集执行。 |

### 事件函数

| 函数 | 什么时候用 |
| --- | --- |
| `registerDatapackRegistry(event)` | 手动注册 datapack registry。正常项目由生成的 MOD bus subscriber 自动调用。 |
| `addReloadVerifier(event)` | 手动挂 reload 校验。正常项目由生成的 FORGE bus subscriber 自动调用。 |

datapack registry 和 reload verifier 默认由生成的 Forge subscriber 自动接线。`HitGraph.registerDatapackRegistry(event)` 和 `HitGraph.addReloadVerifier(event)` 仍然存在，但正常项目不用直接调用。

输入 key 在 `HitGraph.Context`，输出 key 在 `HitGraph.Output`：

```java
HitGraph.Context.BASE;
HitGraph.Context.POWER;
HitGraph.Context.TARGET;
HitGraph.Context.DAMAGE_SOURCE;
HitGraph.Output.DAMAGE_FINAL;
HitGraph.Output.ACCEPTED;
HitGraph.Output.LAST_TARGET;
```

Action contract 不需要手写字符串：

```java
@Override
public PiEngineContextContract contextContract() {
    return PiEngineContextContract.builder()
            .number(HitGraph.Context.BASE)
            .object(HitGraph.Context.TARGET)
            .object(HitGraph.Context.DAMAGE_SOURCE)
            .build();
}
```

内置 emit action 也直接使用 facade key：

```java
new PiEmitNumberAction(
        HitGraph.Output.DAMAGE_FINAL,
        PiDoubleExpression.of("base + power * 2"));
new PiEmitObjectAction(
        HitGraph.Output.LAST_TARGET,
        HitGraph.Context.TARGET);
```

Forge 事件接线由 processor 生成。正常项目只需要保留注解声明，不需要在 mod 构造器里手动挂 listener。

Datagen 默认数据：

```java
PiDataSet<PiEngineAction> defaults = HitGraph.dataSet()
        .entry("fire_hit", new DamageAction(PiDoubleExpression.of("base + power * 2")))
        .build();

new PiDataProvider(output, "ExampleMod Hit Action Data", HitGraph.validationContext(), defaults);
```

给自己模组生成默认数据时用无参 `dataSet()`。给其他 namespace 生成数据时用 `dataSet(namespace)`。

运行：

```java
HitOutput output = HitGraph.run(
        level.registryAccess(),
        "fire_hit",
        new HitInput(base, power, target, source));
```

`fire_hit` 是数据条目 id。处理器不会按数据包文件自动生成 `fireHit(...)` 这种业务方法，因为数据包条目可以由其他包新增、删除或改名。

## 输入输出规则

输入 record：

- primitive 数字和 `Number` 子类会变成 context number。
- 其他引用类型会变成 context object。
- `@PiNumber("key")` 和 `@PiObject("key")` 可以改 context key。
- 生成 binder 会拒绝 null object 和 null `Number`。

输出 record：

- 数字组件会变成 `PiEngineValueKey<Number>`。
- boolean 组件会变成 `PiEngineValueKey<Boolean>`。
- 引用类型组件会变成 typed object value key。
- `@PiOutput("key")` 可以改 frame value key。`damage.final` 这种 dotted output path 可以直接用。

一个 `@PiDataPackRegistry` 只能对应一个 `@PiGraphInput`。如果 `hit`、`projectile`、`machine_tick` 需要不同输入 record，就声明多个 registry 字段。

## 手动注册

不使用注解时，可以手动注册 action datapack registry：

```java
public static final ResourceKey<Registry<PiEngineAction>> HIT_ACTIONS_KEY =
        ResourceKey.createRegistryKey(new ResourceLocation("examplemod", "hit_actions"));

@SubscribeEvent
public static void newDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
    PiDataPackRegistries.action(
            event,
            HIT_ACTIONS_KEY,
            HIT_ACTIONS,
            PiDataPackSync.SYNC_TO_CLIENT);
}
```

`PiDataPackSync.SYNC_TO_CLIENT` 会给 registry 同时提供 network codec。`PiDataPackSync.SERVER_ONLY` 只注册服务端 codec。

手动 runner 需要自己写 binder：

```java
public record HitInput(double base, double power, LivingEntity target, DamageSource source) {
}

public enum HitBinder implements PiEngineContextBinder<HitInput> {
    INSTANCE;

    @Override
    public PiEngineContextContract contract() {
        return PiEngineContextContract.builder()
                .number("base")
                .number("power")
                .object("target", LivingEntity.class)
                .object("damageSource", DamageSource.class)
                .build();
    }

    @Override
    public PiEngineContext bind(HitInput input) {
        return PiEngineContext.builder()
                .number("base", input.base())
                .number("power", input.power())
                .object("target", input.target())
                .object("damageSource", input.source())
                .build();
    }
}
```

执行：

```java
PiEngineRunner<HitInput> runner =
        PiEngineRunner.actionRegistry(HIT_ACTIONS_KEY, HitBinder.INSTANCE);

PiEngineFrame frame = runner.run(
        server.registryAccess(),
        ResourceKey.create(HIT_ACTIONS_KEY, new ResourceLocation("examplemod", "fire_hit")),
        input);
```

也可以从 `PiDataSet<PiEngineAction>` 执行：

```java
PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);
PiEngineFrame frame = runner.run(dataSet, new ResourceLocation("examplemod", "fire_hit"), input);
```

`runner.run(...)` 会先校验 binder contract 和 action contract，然后再执行。

## 验证所有 action

```java
runner.verifyAll(dataSet);
runner.verifyAll(server.registryAccess());
```

验证失败会带上 entry id 和字段路径，例如：

```text
examplemod:fire_hit/context.numbers.power: missing engine context number `power`
```
