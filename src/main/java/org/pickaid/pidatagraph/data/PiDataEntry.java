package org.pickaid.pidatagraph.data;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PiDataEntry<T>(ResourceLocation id, T value) {
    public PiDataEntry {
        id = Objects.requireNonNull(id, "id");
        value = Objects.requireNonNull(value, "value");
    }
}
