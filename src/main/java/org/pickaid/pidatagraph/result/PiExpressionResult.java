package org.pickaid.pidatagraph.result;

import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;

public record PiExpressionResult(String key, double value) implements PiGraphResult {
    public PiExpressionResult {
        key = PiEngineKeyNames.frameValue(key);
    }

    public static PiExpressionResult number(String key, double value) {
        return new PiExpressionResult(key, value);
    }
}
