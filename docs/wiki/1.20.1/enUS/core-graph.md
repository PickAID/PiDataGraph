# Core Graph Execution

`org.pickaid.pidatagraph.core` provides a small directed graph executor. It handles nodes, ports, edges, topology sorting, and execution results.

## Define a Graph

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

## Register Node Types

```java
PiGraphNodeTypeRegistry registry = PiGraphNodeTypeRegistry.builder()
        .register(new PiGraphNodeType(new ResourceLocation("examplemod", "constant"),
                execution -> execution.output(out, 21)))
        .register(new PiGraphNodeType(new ResourceLocation("examplemod", "sink"),
                execution -> execution.output(out, execution.input(in, Integer.class))))
        .build();
```

## Execute

```java
PiGraphExecutionResult result = PiGraphExecutor.execute(graph, registry);
int value = result.output(sink.id(), out, Integer.class);
```

You can validate or plan separately:

```java
PiGraphValidation validation = PiGraphValidator.validate(graph);
PiGraphPlan plan = PiGraphPlanner.plan(graph);
```

Validation currently checks duplicate nodes, missing ports, and cycles.
