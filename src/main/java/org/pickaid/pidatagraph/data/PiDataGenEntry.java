package org.pickaid.pidatagraph.data;

@FunctionalInterface
public interface PiDataGenEntry<T> {
    void register(PiDataSet.Builder<T> builder);
}
