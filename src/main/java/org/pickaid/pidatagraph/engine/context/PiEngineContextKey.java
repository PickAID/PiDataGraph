package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;

public record PiEngineContextKey<T>(String name, Class<T> type) {
    public PiEngineContextKey {
        name = PiEngineKeyNames.object(name);
        type = Objects.requireNonNull(type, "type");
    }

    public static <T> PiEngineContextKey<T> of(String name, Class<T> type) {
        return new PiEngineContextKey<>(name, type);
    }

}
