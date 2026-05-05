package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

public record PiEngineContextKey<T>(String name, Class<T> type) {
    public PiEngineContextKey {
        name = checkName(name);
        type = Objects.requireNonNull(type, "type");
    }

    public static <T> PiEngineContextKey<T> of(String name, Class<T> type) {
        return new PiEngineContextKey<>(name, type);
    }

    private static String checkName(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        PiExpressionScope.of(checked);
        return checked;
    }
}
