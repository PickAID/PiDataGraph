package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicate;

public final class PiEngineActionType<T extends PiEngineAction> {
    private final ResourceLocation id;
    private final BiFunction<Codec<PiEngineAction>, Codec<PiEnginePredicate>, Codec<T>> codecFactory;

    private PiEngineActionType(ResourceLocation id, BiFunction<Codec<PiEngineAction>, Codec<PiEnginePredicate>, Codec<T>> codecFactory) {
        this.id = Objects.requireNonNull(id, "id");
        this.codecFactory = Objects.requireNonNull(codecFactory, "codecFactory");
    }

    public static <T extends PiEngineAction> PiEngineActionType<T> of(
            ResourceLocation id,
            Function<Codec<PiEngineAction>, Codec<T>> codecFactory
    ) {
        Objects.requireNonNull(codecFactory, "codecFactory");
        return new PiEngineActionType<>(id, (actionCodec, predicateCodec) -> codecFactory.apply(actionCodec));
    }

    public static <T extends PiEngineAction> PiEngineActionType<T> of(
            ResourceLocation id,
            BiFunction<Codec<PiEngineAction>, Codec<PiEnginePredicate>, Codec<T>> codecFactory
    ) {
        return new PiEngineActionType<>(id, codecFactory);
    }

    public ResourceLocation id() {
        return id;
    }

    public Codec<T> codec(Codec<PiEngineAction> actionCodec) {
        return codec(actionCodec, org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates.standardCodec());
    }

    public Codec<T> codec(Codec<PiEngineAction> actionCodec, Codec<PiEnginePredicate> predicateCodec) {
        Codec<T> codec = codecFactory.apply(
                Objects.requireNonNull(actionCodec, "actionCodec"),
                Objects.requireNonNull(predicateCodec, "predicateCodec")
        );
        return Objects.requireNonNull(codec, "engine action type " + id + " returned null codec");
    }
}
