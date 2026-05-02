package org.pickaid.pidatagraph.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PiGraphDefinitionTest {
    private static final ResourceLocation GRAPH_ID = id("spell_flow");

    @Test
    void rejectsDuplicateNodeIds() {
        PiGraphNodeId source = new PiGraphNodeId("source");
        PiGraphNode node = new PiGraphNode(source, id("constant"), List.of(), List.of(new PiGraphPortId("out")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new PiGraphDefinition(GRAPH_ID, List.of(node, node), List.of()));

        assertTrue(error.getMessage().contains("duplicate node"));
    }

    @Test
    void reportsMissingPortsBeforeExecution() {
        PiGraphNode source = new PiGraphNode(
                new PiGraphNodeId("source"),
                id("constant"),
                List.of(),
                List.of(new PiGraphPortId("out")));
        PiGraphNode target = new PiGraphNode(
                new PiGraphNodeId("target"),
                id("sink"),
                List.of(new PiGraphPortId("in")),
                List.of());
        PiGraphDefinition graph = new PiGraphDefinition(
                GRAPH_ID,
                List.of(source, target),
                List.of(new PiGraphEdge(
                        new PiGraphEndpoint(source.id(), new PiGraphPortId("missing")),
                        new PiGraphEndpoint(target.id(), new PiGraphPortId("in")))));

        PiGraphValidation validation = PiGraphValidator.validate(graph);

        assertFalse(validation.ok());
        assertEquals(PiGraphIssue.Code.MISSING_OUTPUT_PORT, validation.issues().get(0).code());
    }

    @Test
    void computesStableTopologicalOrder() {
        PiGraphNode source = new PiGraphNode(
                new PiGraphNodeId("source"),
                id("constant"),
                List.of(),
                List.of(new PiGraphPortId("out")));
        PiGraphNode middle = new PiGraphNode(
                new PiGraphNodeId("middle"),
                id("map"),
                List.of(new PiGraphPortId("in")),
                List.of(new PiGraphPortId("out")));
        PiGraphNode target = new PiGraphNode(
                new PiGraphNodeId("target"),
                id("sink"),
                List.of(new PiGraphPortId("in")),
                List.of());
        PiGraphDefinition graph = new PiGraphDefinition(
                GRAPH_ID,
                List.of(target, source, middle),
                List.of(
                        new PiGraphEdge(
                                new PiGraphEndpoint(source.id(), new PiGraphPortId("out")),
                                new PiGraphEndpoint(middle.id(), new PiGraphPortId("in"))),
                        new PiGraphEdge(
                                new PiGraphEndpoint(middle.id(), new PiGraphPortId("out")),
                                new PiGraphEndpoint(target.id(), new PiGraphPortId("in")))));

        PiGraphPlan plan = PiGraphPlanner.plan(graph);

        assertEquals(List.of(source.id(), middle.id(), target.id()), plan.nodeOrder());
    }

    @Test
    void rejectsCycles() {
        PiGraphNode first = new PiGraphNode(
                new PiGraphNodeId("first"),
                id("map"),
                List.of(new PiGraphPortId("in")),
                List.of(new PiGraphPortId("out")));
        PiGraphNode second = new PiGraphNode(
                new PiGraphNodeId("second"),
                id("map"),
                List.of(new PiGraphPortId("in")),
                List.of(new PiGraphPortId("out")));
        PiGraphDefinition graph = new PiGraphDefinition(
                GRAPH_ID,
                List.of(first, second),
                List.of(
                        new PiGraphEdge(
                                new PiGraphEndpoint(first.id(), new PiGraphPortId("out")),
                                new PiGraphEndpoint(second.id(), new PiGraphPortId("in"))),
                        new PiGraphEdge(
                                new PiGraphEndpoint(second.id(), new PiGraphPortId("out")),
                                new PiGraphEndpoint(first.id(), new PiGraphPortId("in")))));

        PiGraphValidation validation = PiGraphValidator.validate(graph);

        assertFalse(validation.ok());
        assertEquals(PiGraphIssue.Code.CYCLE, validation.issues().get(0).code());
        assertThrows(IllegalStateException.class, () -> PiGraphPlanner.plan(graph));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
