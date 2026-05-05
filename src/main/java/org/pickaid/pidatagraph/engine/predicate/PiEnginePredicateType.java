package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public final class PiEnginePredicateType<T extends PiEnginePredicate> {
    private final ResourceLocation id;
    private final Function<Codec<PiEnginePredicate>, Codec<T>> codecFactory;

    private PiEnginePredicateType(ResourceLocation id, Function<Codec<PiEnginePredicate>, Codec<T>> codecFactory) {
        this.id = Objects.requireNonNull(id, "id");
        this.codecFactory = Objects.requireNonNull(codecFactory, "codecFactory");
    }

    public static <T extends PiEnginePredicate> PiEnginePredicateType<T> of(
            ResourceLocation id,
            Function<Codec<PiEnginePredicate>, Codec<T>> codecFactory
    ) {
        return new PiEnginePredicateType<>(id, codecFactory);
    }

    public ResourceLocation id() {
        return id;
    }

    public Codec<T> codec(Codec<PiEnginePredicate> predicateCodec) {
        Codec<T> codec = codecFactory.apply(Objects.requireNonNull(predicateCodec, "predicateCodec"));
        return Objects.requireNonNull(codec, "engine predicate type " + id + " returned null codec");
    }
}
