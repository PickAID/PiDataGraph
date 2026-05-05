package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiEmitObjectAction implements PiEngineAction {
    private final String name;
    private final String source;

    public PiEmitObjectAction(String name, String source) {
        this.name = PiEngineActions.checkFrameValueName(Objects.requireNonNull(name, "name"));
        this.source = PiEngineActions.checkVariableName(Objects.requireNonNull(source, "source"));
    }

    static Codec<PiEmitObjectAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(PiEmitObjectAction::name),
                Codec.STRING.fieldOf("source").forGetter(PiEmitObjectAction::source)
        ).apply(instance, PiEmitObjectAction::new));
    }

    public String name() {
        return name;
    }

    public String source() {
        return source;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.EMIT_OBJECT;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        Object value = context.object(source).orElseThrow(() -> new IllegalArgumentException(
                "missing engine context object: " + source
        ));
        return PiEngineFrame.builder().object(name, value).build();
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(source, Object.class).build();
    }
}
