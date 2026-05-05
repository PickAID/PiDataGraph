package org.pickaid.pidatagraph.engine;

@FunctionalInterface
public interface PiEngineFlow {
    PiEngineFrame run(PiEngineSignal signal);
}
