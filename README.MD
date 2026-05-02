# PiDataGraph

[English](README_EN.md)

`PiDataGraph` 是 Pi 系列的数据流图运行时。它的职责不是替代配置系统，也不是做脚本语言；它负责把一组节点、端口和连线变成可校验、可排序、可执行的数据流。

当前目标是先把核心模型做稳，再接 datapack codec、reload registry 和脚本/调试入口。

## 当前已实现

核心模型：

1. `PiGraphNodeId` / `PiGraphPortId`
2. `PiGraphNode`
3. `PiGraphEndpoint`
4. `PiGraphEdge`
5. `PiGraphDefinition`

校验与计划：

1. 重复 node id 在构造图时直接失败；
2. 缺失 source / target node 会被诊断；
3. 缺失 input / output port 会被诊断；
4. cycle 会被诊断；
5. `PiGraphPlanner` 会生成稳定拓扑顺序。

运行时：

1. `PiGraphNodeType`
2. `PiGraphNodeTypeRegistry`
3. `PiGraphNodeExecution`
4. `PiGraphExecutor`
5. `PiGraphExecutionResult`

执行器会按拓扑顺序执行节点，并把上游 output 沿 edge 送到下游 input。

## 最小示例

```java
PiGraphPortId in = new PiGraphPortId("in");
PiGraphPortId out = new PiGraphPortId("out");

PiGraphNode source = new PiGraphNode(
        new PiGraphNodeId("source"),
        id("constant"),
        List.of(),
        List.of(out));

PiGraphNode target = new PiGraphNode(
        new PiGraphNodeId("target"),
        id("double"),
        List.of(in),
        List.of(out));

PiGraphDefinition graph = new PiGraphDefinition(
        id("example"),
        List.of(source, target),
        List.of(new PiGraphEdge(
                new PiGraphEndpoint(source.id(), out),
                new PiGraphEndpoint(target.id(), in))));

PiGraphNodeTypeRegistry registry = PiGraphNodeTypeRegistry.builder()
        .register(new PiGraphNodeType(id("constant"), execution -> execution.output(out, 21)))
        .register(new PiGraphNodeType(id("double"), execution ->
                execution.output(out, execution.input(in, Integer.class) * 2)))
        .build();

PiGraphExecutionResult result = PiGraphExecutor.execute(graph, registry);
int value = result.output(target.id(), out, Integer.class);
```

## 当前边界

这版先只做 Java runtime core。暂时不把 KubeJS、Architectury 或额外 loader 依赖塞进来，因为核心图模型不需要它们。

下一步应该做：

1. datapack JSON / codec 表达；
2. reload 后的 graph registry；
3. node type 的 schema/port contract；
4. 更好的执行错误上下文；
5. 和 Pibrary config/recipe/reload 体系的消费样例。

## 验证

```bash
bash ./gradlew test --no-daemon
```
