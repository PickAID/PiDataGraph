# 基础图执行

`org.pickaid.pidatagraph.core` 提供一个小型有向图执行器。它处理节点、端口、边、拓扑排序和执行结果。

## 定义图

```java
PiGraphPortId in = new PiGraphPortId("in");
PiGraphPortId out = new PiGraphPortId("out");

PiGraphNode source = new PiGraphNode(
        new PiGraphNodeId("source"),
        new ResourceLocation("examplemod", "constant"),
        List.of(),
        List.of(out));

PiGraphNode sink = new PiGraphNode(
        new PiGraphNodeId("sink"),
        new ResourceLocation("examplemod", "sink"),
        List.of(in),
        List.of(out));

PiGraphDefinition graph = new PiGraphDefinition(
        new ResourceLocation("examplemod", "flow"),
        List.of(source, sink),
        List.of(new PiGraphEdge(
                new PiGraphEndpoint(source.id(), out),
                new PiGraphEndpoint(sink.id(), in))));
```

## 注册节点类型

```java
PiGraphNodeTypeRegistry registry = PiGraphNodeTypeRegistry.builder()
        .register(new PiGraphNodeType(new ResourceLocation("examplemod", "constant"),
                execution -> execution.output(out, 21)))
        .register(new PiGraphNodeType(new ResourceLocation("examplemod", "sink"),
                execution -> execution.output(out, execution.input(in, Integer.class))))
        .build();
```

## 执行

```java
PiGraphExecutionResult result = PiGraphExecutor.execute(graph, registry);
int value = result.output(sink.id(), out, Integer.class);
```

执行前可以单独校验：

```java
PiGraphValidation validation = PiGraphValidator.validate(graph);
PiGraphPlan plan = PiGraphPlanner.plan(graph);
```

当前校验会检查重复节点、缺失端口和环。
