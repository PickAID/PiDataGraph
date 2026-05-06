# 注解生成

PiDataGraph 的注解在编译期生成 datapack registry、runner、context binder、typed key、output mapper 和可选 facade。没有运行时扫描。

推荐给 `@PiGraphInput` 设置 `facade`，让普通代码使用生成的 facade。内部 `_PiDataGraph` 类仍然会生成，但不应该出现在玩法调用、datagen 或 action 实现示例里。

## Gradle 接入

```toml
[dependencies.implementation]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }

[dependencies.jarjar]
pidatagraph = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", range = "[0.0.5,0.1.0)", transitive = false }

[dependencies.annotation_processor]
pidatagraph_processor = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }
```

只写 `implementation` 和 `jarjar` 时，运行时类能用，但不会生成注解代码。

## `HitGraph` 从哪里来

`HitGraph` 不是你手写的类，也不是运行时扫描出来的类。它由 Java annotation processor 在编译时生成：

```java
@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(...) {
}
```

上面这行里的 `facade = "HitGraph"` 决定生成类名。生成类会放在 `@PiDataGraphModule` 所在的 package 里。例如 `ExampleGraphs` 在 `com.examplemod.data`，生成的类就是 `com.examplemod.data.HitGraph`。

不要在源码里新建 `HitGraph.java`。如果自己创建了同名类，processor 会报生成冲突。

## IDEA 能不能识别

可以，但前提是 Gradle 里配置了 `annotationProcessor`。使用本模板时，就是 `project.toml` 里的：

```toml
[dependencies.annotation_processor]
pidatagraph_processor = { notation = "com.mihono.pickaid:pidatagraph:0.0.5-dev", transitive = false }
```

IDEA 正常导入 Gradle 项目后，`HitGraph` 会在编译后出现在 Gradle 的 generated sources 里。第一次写完注解后如果还是红色，通常做这几步：

1. 运行一次 `./gradlew compileJava` 或 `./gradlew test`。
2. 在 IDEA 里 Reload All Gradle Projects。
3. 确认项目没有只写 `implementation` / `jarjar`，而漏掉 `annotation_processor`。

如果 `HitGraph` 和调用代码在同一个 package，不需要 import。如果不在同一个 package，按普通 Java 类 import 生成后的完整包名。

## 完整声明

```java
@PiDataGraphModule(modid = "examplemod")
public final class ExampleGraphs {
    @PiDataPackRegistry(path = "hit_action", folder = "skills/hit", sync = PiDataPackSync.SYNC_TO_CLIENT)
    public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();

    private ExampleGraphs() {
    }
}

@PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
public record HitInput(
        double base,
        @PiNumber("power") Number attackPower,
        @PiObject("target") LivingEntity target,
        DamageSource damageSource
) {
}

@PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
public record HitOutput(
        @PiOutput("damage.final") double damage,
        boolean accepted,
        LivingEntity lastTarget
) {
}
```

这组声明会生成可直接使用的 `HitGraph`：

```java
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);
PiDataSet<PiEngineAction> defaults = HitGraph.dataSet()
        .entry("fire_hit", action)
        .build();
PiDataBuildContext context = HitGraph.validationContext();
```

生成的 key 也在 facade 上：

```java
HitGraph.Context.BASE;
HitGraph.Context.POWER;
HitGraph.Context.TARGET;
HitGraph.Context.DAMAGE_SOURCE;
HitGraph.Output.DAMAGE_FINAL;
HitGraph.Output.ACCEPTED;
HitGraph.Output.LAST_TARGET;
```

## 注解说明

| 注解 | 放在哪里 | 作用 |
| --- | --- | --- |
| `@PiDataGraphModule` | 类 | 声明生成根和 `modid` namespace。 |
| `@PiDataPackRegistry` | `static final PiEngineActionRegistry` 字段 | 生成 datapack registry、data definition、自动 Forge subscriber、手动注册入口和 reload 校验。 |
| `@PiGraphInput` | record | 把 record 组件映射成 runner 输入和 context contract。 |
| `@PiGraphOutput` | record | 把 frame 输出映射回 record，并生成 output typed key。 |
| `@PiNumber` | input record 组件 | 指定 context number key。 |
| `@PiObject` | input record 组件 | 指定 context object key。 |
| `@PiOutput` | output record 组件 | 指定 frame value key，支持 `damage.final` 这种 dotted path。 |

## 生成 Facade 函数

`@PiGraphInput.facade` 是普通代码最应该依赖的入口。下面按用途列出生成的 `HitGraph` 成员。

### ID 函数

| 函数 | 用途 |
| --- | --- |
| `id(String path)` | 把 `"fire_hit"` 变成 `examplemod:fire_hit`。适合 datagen、日志、普通查找，以及只需要 `ResourceLocation` 的 API。 |
| `key(String path)` | 把 `"fire_hit"` 变成 datapack registry 里的 `ResourceKey<PiEngineAction>`。适合同步、registry API，以及明确要求 `ResourceKey<?>` 的 API。 |

### Datagen 函数

| 函数 | 用途 |
| --- | --- |
| `dataSet()` | 创建当前 mod namespace 下的默认数据 builder。给自己模组生成 JSON 时使用。 |
| `dataSet(String namespace)` | 创建指定 namespace 下的数据 builder。兼容包或测试想写进别的 namespace 时使用。 |
| `validationContext()` | 返回 runner 的校验上下文。传给 `PiDataProvider`，让 datagen 写文件前检查表达式、输入 key 和 action contract。 |

### 运行函数

| 函数 | 用途 |
| --- | --- |
| `run(RegistryAccess access, String path, HitInput input)` | 从服务器当前 datapack registry 读取并执行。玩法代码最常用。 |
| `run(RegistryAccess access, ResourceLocation id, HitInput input)` | 已经有完整 id 时使用，例如 id 来自配置或同步 payload。 |
| `run(RegistryAccess access, ResourceKey<?> key, HitInput input)` | 已经有 registry entry key 时使用，例如和其他 registry/sync API 串接。 |
| `run(PiDataSet<PiEngineAction> actions, String path, HitInput input)` | 从临时数据集执行。测试和无世界工具最常用。 |
| `run(PiDataSet<PiEngineAction> actions, ResourceLocation id, HitInput input)` | 临时数据集执行，并且调用方已经有完整 id。 |
| `run(PiDataSet<PiEngineAction> actions, ResourceKey<?> key, HitInput input)` | 临时数据集执行，并且调用方已经有 entry key。 |

### 事件函数

| 函数 | 用途 |
| --- | --- |
| `registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event)` | 手动注册入口。正常项目由生成的 MOD bus subscriber 自动调用它。 |
| `addReloadVerifier(AddReloadListenerEvent event)` | 手动 reload 校验入口。正常项目由生成的 FORGE bus subscriber 自动调用它。 |

正常项目声明 `@PiDataPackRegistry` 后不用再写 `modBus.addListener(...)`。手动事件函数保留，是为了测试、特殊加载流程和需要自己控制事件接线的项目。

### Key 分组

| 成员 | 用途 |
| --- | --- |
| `Context` | 输入 key，例如 `HitGraph.Context.BASE`、`HitGraph.Context.TARGET`。action contract 和读取 context 时使用。 |
| `Output` | 输出 key，例如 `HitGraph.Output.DAMAGE_FINAL`。emit action 和读取 frame 时使用。 |

## 默认映射

`@PiGraphInput` 的 record 组件会这样映射：

| Java 类型 | 默认映射 |
| --- | --- |
| primitive number，例如 `int`、`double` | context number |
| `Number` 子类，例如 `Integer`、`BigDecimal` | context number，binder 会拒绝 null |
| 非 primitive reference type | typed context object |

`@PiGraphOutput` 的 record 组件会这样映射：

| Java 类型 | 默认映射 |
| --- | --- |
| number 类型 | `PiEngineValueKey<Number>` |
| `boolean` / `Boolean` | `PiEngineValueKey<Boolean>` |
| 其他 reference type | typed object value key |

## 约束

`@PiDataGraphModule` 类不能是 private，外层类也不能是 private。`modid` 必须是合法 ResourceLocation namespace。

`@PiDataPackRegistry` 字段必须是 `static final PiEngineActionRegistry`，不能是 private，必须放在 `@PiDataGraphModule` 类里。`path` 必须是合法 ResourceLocation path，`folder` 必须是相对 data folder。

`@PiGraphInput` 和 `@PiGraphOutput` 只能放在 record 上，record 和外层类不能是 private。跨 package 使用时，record 和涉及到的 object 类型必须 public 可访问。

一个 datapack registry 只能绑定一个 `@PiGraphInput`。如果两个运行流程需要不同输入 record，就声明两个 `@PiDataPackRegistry` 字段。

context key 必须是表达式变量名。output key 可以是普通变量名或 dotted path。重复 key、重复生成字段名、输入输出字段名冲突都会在编译期报错。
