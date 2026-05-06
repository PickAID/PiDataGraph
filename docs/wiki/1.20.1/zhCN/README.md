# PiDataGraph 1.20.1

PiDataGraph 现在提供一套小型但完整的数据驱动运行层：用 Codec 读取 JSON，用表达式描述数值，用 `PiEngineContext` 接入真实游戏代码，并用 runner 统一执行和校验。

## 入口

- [数据文件](data.md)
- [执行上下文](context.md)
- [Action 和 Predicate](actions.md)
- [注解生成](annotations.md)
- [数据包注册表和 Runner](registry-runner.md)
- [基础图执行](core-graph.md)
- [同步桥接](sync.md)

## 最短接入链路

1. 用 `@PiDataGraphModule` 和 `@PiDataPackRegistry` 声明 datapack action registry。
2. 用 `@PiGraphInput(..., facade = "HitGraph")` 和 `@PiGraphOutput` 声明运行输入和输出。
3. 编译器会生成 Forge subscriber，自动注册 datapack registry 和 reload verifier。
4. datagen 用 `HitGraph.dataSet()` 和 `HitGraph.validationContext()` 生成默认 JSON。
5. 运行时调用 `HitGraph.run(level.registryAccess(), "entry_id", input)` 并读取 output record。

```java
HitInput input = new HitInput(base, power, target, source);
HitOutput output = HitGraph.run(level.registryAccess(), "fire_hit", input);
```
