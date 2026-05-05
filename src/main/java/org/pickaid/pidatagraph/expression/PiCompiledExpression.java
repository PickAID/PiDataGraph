package org.pickaid.pidatagraph.expression;

import java.util.Objects;
import net.objecthunter.exp4j.Expression;

public final class PiCompiledExpression {
    private final String source;
    private final PiExpressionScope scope;
    private final Expression expression;
    private final ThreadLocal<PiExpressionContext> activeContext;

    PiCompiledExpression(
            String source,
            PiExpressionScope scope,
            Expression expression,
            ThreadLocal<PiExpressionContext> activeContext
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.expression = Objects.requireNonNull(expression, "expression");
        this.activeContext = Objects.requireNonNull(activeContext, "activeContext");
    }

    public String source() {
        return source;
    }

    public PiExpressionScope scope() {
        return scope;
    }

    public synchronized double evaluate(PiExpressionContext context) {
        Objects.requireNonNull(context, "context");
        for (String variable : scope.variables()) {
            if (!context.variables().containsKey(variable)) {
                throw new IllegalArgumentException("missing expression variable `" + variable + "` while evaluating `"
                        + source + "`; available variables: " + context.variables().keySet());
            }
        }
        activeContext.set(context);
        try {
            expression.setVariables(context.variables());
            return expression.evaluate();
        } finally {
            activeContext.remove();
        }
    }
}
