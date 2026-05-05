package org.pickaid.pidatagraph.engine;

@FunctionalInterface
public interface PiEngineModule {
    PiEngineFrame evaluate(PiEngineContext context);
}
