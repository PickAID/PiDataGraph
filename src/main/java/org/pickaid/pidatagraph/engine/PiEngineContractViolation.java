package org.pickaid.pidatagraph.engine;

public final class PiEngineContractViolation extends IllegalStateException {
    public PiEngineContractViolation(String message) {
        super(message);
    }
}
