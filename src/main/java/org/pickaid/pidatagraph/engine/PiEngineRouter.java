package org.pickaid.pidatagraph.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class PiEngineRouter {
    private final Map<ResourceLocation, List<PiEngineFlow>> routes;

    private PiEngineRouter(Map<ResourceLocation, List<PiEngineFlow>> routes) {
        LinkedHashMap<ResourceLocation, List<PiEngineFlow>> copy = new LinkedHashMap<>();
        routes.forEach((type, flows) -> copy.put(type, List.copyOf(flows)));
        this.routes = Map.copyOf(copy);
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiEngineFrame dispatch(PiEngineSignal signal) {
        Objects.requireNonNull(signal, "signal");
        List<PiEngineFlow> flows = routes.get(signal.type());
        if (flows == null || flows.isEmpty()) {
            return PiEngineFrame.empty();
        }
        PiEngineFrame merged = PiEngineFrame.empty();
        for (PiEngineFlow flow : flows) {
            merged = merged.merge(flow.run(signal));
        }
        return merged;
    }

    public static final class Builder {
        private final LinkedHashMap<ResourceLocation, List<PiEngineFlow>> routes = new LinkedHashMap<>();

        public Builder route(ResourceLocation signalType, PiEngineFlow flow) {
            routes.computeIfAbsent(Objects.requireNonNull(signalType, "signalType"), ignored -> new ArrayList<>())
                    .add(Objects.requireNonNull(flow, "flow"));
            return this;
        }

        public PiEngineRouter build() {
            return new PiEngineRouter(routes);
        }
    }
}
