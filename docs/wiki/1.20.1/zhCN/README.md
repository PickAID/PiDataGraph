# PiDataGraph 1.20.1 Wiki

PiDataGraph 的 1.20.1 文档按使用场景拆开：

- [执行上下文](context.md)：`PiEngineContext`、常用 key、Minecraft 对象绑定。
- [公式和数据](data.md)：表达式、datapack JSON、datagen、加载校验。
- [动作链](actions.md)：内置 action、predicate、自定义 Java 叶子 action。
- [同步](sync.md)：如何通过 PiSerializeKit 和 PiNet 同步图状态。
- [下游接入增强方案](authoring-dx-plan.md)：显式 runner/binder 和注解生成方案。

## 最短路径

1. 定义 JSON 数据结构，字段里使用 `PiDoubleExpression` / `PiBooleanExpression`。
2. 用 `PiDataDefinition` 声明 codec 和校验。
3. 用 `PiEngineContentType` 或 `PiEngineActionData` 在加载阶段编译或校验。
4. 执行时用 `PiEngineContext.builder()` 放入本次运行需要的 number 和 object；Java 侧稳定 key 建议用 `PiEngineNumberKey` 和 `PiEngineContextKey<T>` 收成常量。

```java
public static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
public static final PiEngineNumberKey POWER = PiEngineNumberKey.of("power");

PiEngineContext context = PiEngineContext.builder()
        .number(BASE_DAMAGE, 6)
        .number(POWER, 3)
        .number("resource", 20)
        .number("cost", 5)
        .object("actor", player)
        .object("target", target)
        .build();
```

这套系统的重点不是替代 Java 逻辑，而是把“可配置的计算、条件和流程组合”从硬编码里拿出来，并在加载阶段提前发现错误。
