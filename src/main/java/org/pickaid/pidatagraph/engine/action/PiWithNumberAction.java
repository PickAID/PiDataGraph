package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiWithNumberAction implements PiEngineAction {
    private final String name;
    private final PiDoubleExpression value;
    private final PiEngineAction child;

    public PiWithNumberAction(String name, PiDoubleExpression value, PiEngineAction child) {
        this.name = PiEngineActions.checkVariableName(Objects.requireNonNull(name, "name"));
        this.value = Objects.requireNonNull(value, "value");
        this.child = Objects.requireNonNull(child, "child");
    }

    public PiWithNumberAction(PiEngineNumberKey name, PiDoubleExpression value, PiEngineAction child) {
        this(Objects.requireNonNull(name, "name").name(), value, child);
    }

    static Codec<PiWithNumberAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(PiWithNumberAction::name),
                PiDoubleExpression.CODEC.fieldOf("value").forGetter(PiWithNumberAction::value),
                actionCodec.fieldOf("child").forGetter(PiWithNumberAction::child)
        ).apply(instance, PiWithNumberAction::new));
    }

    public String name() {
        return name;
    }

    public PiDoubleExpression value() {
        return value;
    }

    public PiEngineAction child() {
        return child;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.WITH_NUMBER;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        double evaluated = context.evaluate(value);
        return context.derive()
                .number(name, evaluated)
                .build()
                .execute(child);
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineActions.contextContract("with_number child", child).withoutNumber(name);
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verify(path + ".value", context, value);
        child.verify(context.withVariable(name), path + ".child");
    }
}
