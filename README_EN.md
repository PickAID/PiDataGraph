# PiDataGraph

[中文](README.MD)

`PiDataGraph` is the Pi stack data-flow graph runtime. It is not a config replacement and not a scripting language. Its job is to turn nodes, ports, and edges into a graph that can be validated, planned, and executed.

The current direction is to harden the core model first, then add datapack codecs, reload registries, and script/debug entry points.

## Implemented

Core model:

1. `PiGraphNodeId` / `PiGraphPortId`
2. `PiGraphNode`
3. `PiGraphEndpoint`
4. `PiGraphEdge`
5. `PiGraphDefinition`

Validation and planning:

1. duplicate node ids fail during graph construction;
2. missing source / target nodes are reported;
3. missing input / output ports are reported;
4. cycles are reported;
5. `PiGraphPlanner` creates a stable topological order.

Runtime:

1. `PiGraphNodeType`
2. `PiGraphNodeTypeRegistry`
3. `PiGraphNodeExecution`
4. `PiGraphExecutor`
5. `PiGraphExecutionResult`

The executor runs nodes in topological order and routes upstream outputs into downstream inputs through graph edges.

## Minimal Example

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

## Current Boundary

This phase only ships the Java runtime core. KubeJS, Architectury, and extra loader dependencies are intentionally not pulled in because the core graph model does not need them.

Next work should cover:

1. datapack JSON / codec representation;
2. graph registry after reload;
3. node type schema/port contracts;
4. better execution error context;
5. consumer examples with Pibrary config/recipe/reload flows.

## Verification

```bash
bash ./gradlew test --no-daemon
```
