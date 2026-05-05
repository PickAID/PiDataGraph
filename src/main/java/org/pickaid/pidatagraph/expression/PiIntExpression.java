package org.pickaid.pidatagraph.expression;

import com.mojang.serialization.Codec;
import java.util.Objects;

public final class PiIntExpression {
    public static final Codec<PiIntExpression> CODEC = Codec.STRING.xmap(PiIntExpression::of, PiIntExpression::source);

    private final String source;

    private PiIntExpression(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public static PiIntExpression of(String source) {
        return new PiIntExpression(source);
    }

    public static PiIntExpression constant(int value) {
        return of(Integer.toString(value));
    }

    public String source() {
        return source;
    }

    public PiCompiledIntExpression compile(PiExpressionLanguage language, PiExpressionScope scope) {
        return new PiCompiledIntExpression(language.compile(source, scope));
    }

    @Override
    public String toString() {
        return source;
    }
}
