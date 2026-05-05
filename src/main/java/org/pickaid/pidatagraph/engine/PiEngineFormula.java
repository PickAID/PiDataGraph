package org.pickaid.pidatagraph.engine;

@FunctionalInterface
public interface PiEngineFormula<T> {
    T evaluate(PiEngineContext context);
}
