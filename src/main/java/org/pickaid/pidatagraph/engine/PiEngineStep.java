package org.pickaid.pidatagraph.engine;

@FunctionalInterface
public interface PiEngineStep {
    PiEngineFrame evaluate(PiEngineContext context, PiEngineFrame previous);
}
