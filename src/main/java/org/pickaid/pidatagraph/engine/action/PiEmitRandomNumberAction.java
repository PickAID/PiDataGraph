package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiEmitRandomNumberAction implements PiEngineAction {
    static final Codec<PiEmitRandomNumberAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(PiEmitRandomNumberAction::name),
            PiDoubleExpression.CODEC.fieldOf("min").forGetter(PiEmitRandomNumberAction::min),
            PiDoubleExpression.CODEC.fieldOf("max").forGetter(PiEmitRandomNumberAction::max)
    ).apply(instance, PiEmitRandomNumberAction::new));

    private final String name;
    private final PiDoubleExpression min;
    private final PiDoubleExpression max;

    public PiEmitRandomNumberAction(String name, PiDoubleExpression min, PiDoubleExpression max) {
        this.name = PiEngineActions.checkFrameValueName(Objects.requireNonNull(name, "name"));
        this.min = Objects.requireNonNull(min, "min");
        this.max = Objects.requireNonNull(max, "max");
    }

    public PiEmitRandomNumberAction(PiEngineValueKey<? extends Number> name, PiDoubleExpression min, PiDoubleExpression max) {
        this(Objects.requireNonNull(name, "name").name(), min, max);
    }

    public String name() {
        return name;
    }

    public PiDoubleExpression min() {
        return min;
    }

    public PiDoubleExpression max() {
        return max;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.EMIT_RANDOM_NUMBER;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        double lower = context.evaluate(min);
        double upper = context.evaluate(max);
        return PiEngineFrame.builder()
                .number(name, context.random() * (upper - lower) + lower)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verify(path + ".min", context, min);
        PiEngineActions.verify(path + ".max", context, max);
    }
}
