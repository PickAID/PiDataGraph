package org.pickaid.pidatagraph.expression;

@FunctionalInterface
public interface PiExpressionFunction {
    double apply(PiExpressionContext context, double... args);
}
