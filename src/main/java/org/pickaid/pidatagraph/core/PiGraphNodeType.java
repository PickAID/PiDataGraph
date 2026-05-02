package org.pickaid.pidatagraph.core;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PiGraphNodeType(
        ResourceLocation id,
        PiGraphNodeAction action
) {
    public PiGraphNodeType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(action, "action");
    }
}
