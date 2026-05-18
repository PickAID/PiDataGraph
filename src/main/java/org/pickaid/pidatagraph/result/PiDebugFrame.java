package org.pickaid.pidatagraph.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PiDebugFrame(
        ResourceLocation graphId,
        String step,
        List<PiGraphResult> results,
        Map<String, Object> values
) implements PiGraphResult {
    public PiDebugFrame {
        graphId = Objects.requireNonNull(graphId, "graphId");
        step = Objects.requireNonNull(step, "step");
        results = List.copyOf(Objects.requireNonNull(results, "results"));
        values = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(values, "values")));
    }

    public static PiDebugFrame empty(ResourceLocation graphId, String step) {
        return new PiDebugFrame(graphId, step, List.of(), Map.of());
    }
}
