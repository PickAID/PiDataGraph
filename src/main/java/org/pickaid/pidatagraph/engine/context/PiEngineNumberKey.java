package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

/**
 * Stable Java-side handle for a numeric expression variable.
 */
public record PiEngineNumberKey(String name) {
    public PiEngineNumberKey {
        name = checkName(name);
    }

    public static PiEngineNumberKey of(String name) {
        return new PiEngineNumberKey(name);
    }

    private static String checkName(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        PiExpressionScope.of(checked);
        return checked;
    }
}
