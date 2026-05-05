package org.pickaid.pidatagraph.expression;

import java.util.Objects;

public final class PiCompiledBooleanExpression {
    private final PiCompiledExpression expression;

    PiCompiledBooleanExpression(PiCompiledExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public boolean evaluate(PiExpressionContext context) {
        return expression.evaluate(context) > 0.5;
    }

    public PiCompiledExpression expression() {
        return expression;
    }
}
