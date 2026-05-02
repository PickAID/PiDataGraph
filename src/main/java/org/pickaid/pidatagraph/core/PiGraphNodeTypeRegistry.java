package org.pickaid.pidatagraph.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class PiGraphNodeTypeRegistry {
    private static final PiGraphNodeTypeRegistry EMPTY = new PiGraphNodeTypeRegistry(Map.of());

    private final Map<ResourceLocation, PiGraphNodeType> types;

    private PiGraphNodeTypeRegistry(Map<ResourceLocation, PiGraphNodeType> types) {
        this.types = Map.copyOf(types);
    }

    public static PiGraphNodeTypeRegistry empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiGraphNodeType require(ResourceLocation id) {
        PiGraphNodeType type = types.get(Objects.requireNonNull(id, "id"));
        if (type == null) {
            throw new IllegalStateException("missing graph node type: " + id);
        }
        return type;
    }

    public static final class Builder {
        private final Map<ResourceLocation, PiGraphNodeType> types = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder register(PiGraphNodeType type) {
            PiGraphNodeType checked = Objects.requireNonNull(type, "type");
            if (types.putIfAbsent(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate graph node type: " + checked.id());
            }
            return this;
        }

        public PiGraphNodeTypeRegistry build() {
            return new PiGraphNodeTypeRegistry(types);
        }
    }
}
