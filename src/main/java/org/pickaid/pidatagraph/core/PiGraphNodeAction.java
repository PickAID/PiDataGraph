package org.pickaid.pidatagraph.core;

@FunctionalInterface
public interface PiGraphNodeAction {
    void execute(PiGraphNodeExecution execution);
}
