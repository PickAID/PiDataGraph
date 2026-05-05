package org.pickaid.pidatagraph.data;

@FunctionalInterface
public interface PiDataVerifier<T> {
    void verify(PiDataBuildContext context, T value);
}
