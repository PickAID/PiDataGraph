package org.pickaid.pidatagraph.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PiGraphRuntimeTest {
    @Test
    void rejectsDuplicateNodeTypeIds() {
        ResourceLocation type = id("constant");
        PiGraphNodeType first = new PiGraphNodeType(type, execution -> execution.output(new PiGraphPortId("out"), 1));
        PiGraphNodeType second = new PiGraphNodeType(type, execution -> execution.output(new PiGraphPortId("out"), 2));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                PiGraphNodeTypeRegistry.builder().register(first).register(second).build());

        assertEquals("duplicate graph node type: test:constant", error.getMessage());
    }

    @Test
    void executesNodesInTopologyOrderAndRoutesEdgeValues() {
        PiGraphPortId in = new PiGraphPortId("in");
        PiGraphPortId out = new PiGraphPortId("out");
        ResourceLocation constantType = id("constant");
        ResourceLocation doubleType = id("double");
        ResourceLocation sinkType = id("sink");
        PiGraphNode source = new PiGraphNode(new PiGraphNodeId("source"), constantType, List.of(), List.of(out));
        PiGraphNode middle = new PiGraphNode(new PiGraphNodeId("middle"), doubleType, List.of(in), List.of(out));
        PiGraphNode sink = new PiGraphNode(new PiGraphNodeId("sink"), sinkType, List.of(in), List.of(out));
        PiGraphDefinition graph = new PiGraphDefinition(
                id("flow"),
                List.of(sink, middle, source),
                List.of(
                        new PiGraphEdge(new PiGraphEndpoint(source.id(), out), new PiGraphEndpoint(middle.id(), in)),
                        new PiGraphEdge(new PiGraphEndpoint(middle.id(), out), new PiGraphEndpoint(sink.id(), in))));
        PiGraphNodeTypeRegistry registry = PiGraphNodeTypeRegistry.builder()
                .register(new PiGraphNodeType(constantType, execution -> execution.output(out, 21)))
                .register(new PiGraphNodeType(doubleType, execution -> execution.output(out, execution.input(in, Integer.class) * 2)))
                .register(new PiGraphNodeType(sinkType, execution -> execution.output(out, execution.input(in, Integer.class))))
                .build();

        PiGraphExecutionResult result = PiGraphExecutor.execute(graph, registry);

        assertEquals(42, result.output(sink.id(), out, Integer.class));
        assertEquals(List.of(source.id(), middle.id(), sink.id()), result.plan().nodeOrder());
    }

    @Test
    void failsWhenNodeTypeIsMissing() {
        PiGraphPortId out = new PiGraphPortId("out");
        PiGraphNode source = new PiGraphNode(new PiGraphNodeId("source"), id("missing"), List.of(), List.of(out));
        PiGraphDefinition graph = new PiGraphDefinition(id("flow"), List.of(source), List.of());

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                PiGraphExecutor.execute(graph, PiGraphNodeTypeRegistry.empty()));

        assertEquals("missing graph node type: test:missing", error.getMessage());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
