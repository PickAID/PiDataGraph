package org.pickaid.pidatagraph.engine.context;

/**
 * Stable Java-side handle for a numeric expression variable.
 */
public record PiEngineNumberKey(String name) {
    public PiEngineNumberKey {
        name = PiEngineKeyNames.variable(name);
    }

    public static PiEngineNumberKey of(String name) {
        return new PiEngineNumberKey(name);
    }

}
