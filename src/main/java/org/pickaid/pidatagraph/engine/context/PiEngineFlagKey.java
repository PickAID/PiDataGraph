package org.pickaid.pidatagraph.engine.context;

/**
 * Stable Java-side handle for a boolean frame output.
 */
public record PiEngineFlagKey(String name) {
    public PiEngineFlagKey {
        name = PiEngineKeyNames.frameValue(name);
    }

    public static PiEngineFlagKey of(String name) {
        return new PiEngineFlagKey(name);
    }

}
