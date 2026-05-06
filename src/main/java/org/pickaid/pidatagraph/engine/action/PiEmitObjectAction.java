package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;

public final class PiEmitObjectAction implements PiEngineAction {
    private final String name;
    private final String source;
    private final Class<?> sourceType;

    public PiEmitObjectAction(String name, String source) {
        this(name, source, Object.class);
    }

    public <T> PiEmitObjectAction(PiEngineValueKey<T> name, PiEngineContextKey<? extends T> source) {
        this(
                Objects.requireNonNull(name, "name").name(),
                Objects.requireNonNull(source, "source").name(),
                source.type());
    }

    private PiEmitObjectAction(String name, String source, Class<?> sourceType) {
        this.name = PiEngineActions.checkFrameValueName(Objects.requireNonNull(name, "name"));
        this.source = PiEngineActions.checkVariableName(Objects.requireNonNull(source, "source"));
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        if (sourceType.isPrimitive()) {
            throw new IllegalArgumentException("engine context object `" + this.source
                    + "` type must not be primitive: " + sourceType.getName());
        }
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
        Object value = context.requireObject(source, sourceType);
        return PiEngineFrame.builder().object(name, value).build();
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(source, sourceType).build();
    }
}
