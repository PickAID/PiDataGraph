package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

/**
 * Stable Java-side handle for a boolean frame output.
 */
public record PiEngineFlagKey(String name) {
    public PiEngineFlagKey {
        name = checkName(name);
    }

    public static PiEngineFlagKey of(String name) {
        return new PiEngineFlagKey(name);
    }

    private static String checkName(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        PiExpressionScope.of(checked);
        return checked;
    }
}
