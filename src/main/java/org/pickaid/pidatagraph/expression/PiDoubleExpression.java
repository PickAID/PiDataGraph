package org.pickaid.pidatagraph.expression;

import com.mojang.serialization.Codec;
import java.util.Objects;

public final class PiDoubleExpression {
    public static final Codec<PiDoubleExpression> CODEC = Codec.STRING.xmap(PiDoubleExpression::of, PiDoubleExpression::source);

    private final String source;

    private PiDoubleExpression(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public static PiDoubleExpression of(String source) {
        return new PiDoubleExpression(source);
    }

    public static PiDoubleExpression constant(double value) {
        return of(Double.toString(value));
    }

    public String source() {
        return source;
    }

    public PiCompiledDoubleExpression compile(PiExpressionLanguage language, PiExpressionScope scope) {
        return new PiCompiledDoubleExpression(language.compile(source, scope));
    }

    @Override
    public String toString() {
        return source;
    }
}
