package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

/**
 * Shared name validation for engine context and frame keys.
 */
public final class PiEngineKeyNames {
    private PiEngineKeyNames() {
    }

    /**
     * Validates a name that is also an expression variable.
     *
     * <p>Context numbers use this form because formulas read them directly.</p>
     *
     * @param name authored key name
     * @return trimmed key name
     */
    public static String variable(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        PiExpressionScope.of(checked);
        return checked;
    }

    /**
     * Validates a context object key.
     *
     * @param name authored key name
     * @return trimmed key name
     */
    public static String object(String name) {
        return variable(name);
    }

    /**
     * Validates a frame output name.
     *
     * <p>Frame outputs may use dotted paths such as {@code hud.mana_fill}. Each
     * segment still follows expression variable naming rules.</p>
     *
     * @param name authored frame output name
     * @return trimmed frame output name
     */
    public static String frameValue(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        String[] segments = checked.split("\\.", -1);
        for (String segment : segments) {
            variable(segment);
        }
        return checked;
    }
}
