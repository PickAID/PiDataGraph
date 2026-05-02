package org.pickaid.pidatagraph.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PiGraphNode(
        PiGraphNodeId id,
        ResourceLocation type,
        List<PiGraphPortId> inputs,
        List<PiGraphPortId> outputs
) {
    public PiGraphNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        inputs = copyUnique(inputs, "input port");
        outputs = copyUnique(outputs, "output port");
    }

    public boolean hasInput(PiGraphPortId port) {
        return inputs.contains(port);
    }

    public boolean hasOutput(PiGraphPortId port) {
        return outputs.contains(port);
    }

    private static List<PiGraphPortId> copyUnique(List<PiGraphPortId> ports, String label) {
        Objects.requireNonNull(ports, label + "s");
        LinkedHashSet<PiGraphPortId> unique = new LinkedHashSet<>();
        for (PiGraphPortId port : ports) {
            PiGraphPortId checked = Objects.requireNonNull(port, label);
            if (!unique.add(checked)) {
                throw new IllegalArgumentException("duplicate " + label + ": " + checked);
            }
        }
        return List.copyOf(unique);
    }
}
