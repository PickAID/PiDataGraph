package org.pickaid.pidatagraph.expression;

@FunctionalInterface
public interface PiExpressionBinaryOperator {
    double apply(double left, double right);
}
