# PiDataGraph 1.20.1

PiDataGraph 现在提供一套小型但完整的数据驱动运行层：用 Codec 读取 JSON，用表达式描述数值，用 action/predicate 描述流程，用 `PiGraphContext` 把 typed 游戏输入接入图执行。

## 入口

- [数据和表达式](data.md)
- [执行上下文](context.md)
- [动作和条件](actions.md)
- [数据包注册表和 Runner](registry-runner.md)
- [基础图执行](core-graph.md)
- [同步桥接](sync.md)

## 最短接入链路

1. 用 `PiDoubleExpression`、`PiIntExpression`、`PiBooleanExpression` 写数据模型字段。
2. 用 `PiDataDefinition` 声明 JSON 文件夹、Codec 和校验。
3. 用 `PiDataReloadListener` 读取普通 JSON，或用 `PiDataPackRegistries.action(...)` 注册 Minecraft 数据包注册表。
4. 运行时把事件、实体、物品、方块实体等输入绑定成 `PiGraphContext`。
5. 执行 action 或已编译内容，然后从 `PiEngineFrame` 读取结果。

```java
PiGraphContext context = PiGraphContext.builder()
        .number("baseDamage", 6)
        .number("spellPower", 3)
        .object("actor", player)
        .object("target", target)
        .build();
```
