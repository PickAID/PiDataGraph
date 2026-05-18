package org.pickaid.pidatagraph.graphcontext;

import java.util.Objects;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;

public record PiGraphContextKey<T>(String name, Class<T> type) {
    public PiGraphContextKey {
        name = PiEngineKeyNames.object(name);
        type = Objects.requireNonNull(type, "type");
    }

    public static <T> PiGraphContextKey<T> of(String name, Class<T> type) {
        return new PiGraphContextKey<>(name, type);
    }
}
