package org.pickaid.pidatagraph.engine;

import org.pickaid.pidatagraph.data.PiDataEntry;

@FunctionalInterface
public interface PiEngineContentCompiler<S, R> {
    R compile(PiDataEntry<S> entry, PiEngineBuildContext context);
}
