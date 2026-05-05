package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiEmitNumberAction implements PiEngineAction {
    private final String name;
    private final PiDoubleExpression value;

    public PiEmitNumberAction(String name, PiDoubleExpression value) {
        this.name = PiEngineActions.checkVariableName(Objects.requireNonNull(name, "name"));
        this.value = Objects.requireNonNull(value, "value");
    }

    static Codec<PiEmitNumberAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(PiEmitNumberAction::name),
                PiDoubleExpression.CODEC.fieldOf("value").forGetter(PiEmitNumberAction::value)
        ).apply(instance, PiEmitNumberAction::new));
    }

    public String name() {
        return name;
    }

    public PiDoubleExpression value() {
        return value;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.EMIT_NUMBER;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        return PiEngineFrame.builder().number(name, context.evaluate(value)).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verify(path + ".value", context, value);
    }
}
