package org.pickaid.pidatagraph.expression;

import java.util.Objects;

public final class PiCompiledDoubleExpression {
    private final PiCompiledExpression expression;

    PiCompiledDoubleExpression(PiCompiledExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public double evaluate(PiExpressionContext context) {
        return expression.evaluate(context);
    }

    public PiCompiledExpression expression() {
        return expression;
    }
}
