package org.pickaid.pidatagraph.expression;

import java.util.Objects;

public final class PiCompiledIntExpression {
    private final PiCompiledExpression expression;

    PiCompiledIntExpression(PiCompiledExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public int evaluate(PiExpressionContext context) {
        return (int) expression.evaluate(context);
    }

    public PiCompiledExpression expression() {
        return expression;
    }
}
