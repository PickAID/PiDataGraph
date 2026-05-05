package org.pickaid.pidatagraph.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PiEnginePlan {
    private final List<PiEngineStep> steps;

    private PiEnginePlan(List<PiEngineStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiEngineFrame evaluate(PiEngineContext context) {
        Objects.requireNonNull(context, "context");
        PiEngineFrame frame = PiEngineFrame.empty();
        for (PiEngineStep step : steps) {
            frame = frame.merge(step.evaluate(context, frame));
        }
        return frame;
    }

    public static final class Builder {
        private final List<PiEngineStep> steps = new ArrayList<>();

        public Builder run(PiEngineModule module) {
            Objects.requireNonNull(module, "module");
            steps.add((context, frame) -> module.evaluate(context));
            return this;
        }

        public Builder run(PiEngineStep step) {
            steps.add(Objects.requireNonNull(step, "step"));
            return this;
        }

        public PiEnginePlan build() {
            return new PiEnginePlan(steps);
        }
    }
}
