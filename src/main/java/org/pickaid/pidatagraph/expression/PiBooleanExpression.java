package org.pickaid.pidatagraph.expression;

import com.mojang.serialization.Codec;
import java.util.Objects;

public final class PiBooleanExpression {
    public static final Codec<PiBooleanExpression> CODEC = Codec.STRING.xmap(PiBooleanExpression::of, PiBooleanExpression::source);

    private final String source;

    private PiBooleanExpression(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public static PiBooleanExpression of(String source) {
        return new PiBooleanExpression(source);
    }

    public static PiBooleanExpression constant(boolean value) {
        return of(value ? "1" : "0");
    }

    public String source() {
        return source;
    }

    public PiCompiledBooleanExpression compile(PiExpressionLanguage language, PiExpressionScope scope) {
        return new PiCompiledBooleanExpression(language.compile(source, scope));
    }

    @Override
    public String toString() {
        return source;
    }
}
