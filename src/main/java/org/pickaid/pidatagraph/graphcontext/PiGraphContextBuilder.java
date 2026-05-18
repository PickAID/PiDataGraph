package org.pickaid.pidatagraph.graphcontext;

import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import net.minecraft.util.RandomSource;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;

public final class PiGraphContextBuilder {
    private final PiGraphContext.Builder delegate;

    private PiGraphContextBuilder(PiGraphContext.Builder delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static PiGraphContextBuilder create() {
        return new PiGraphContextBuilder(PiGraphContext.builder());
    }

    public PiGraphContextBuilder language(PiExpressionLanguage language) {
        delegate.language(language);
        return this;
    }

    public PiGraphContextBuilder number(String key, double value) {
        delegate.number(key, value);
        return this;
    }

    public PiGraphContextBuilder number(PiEngineNumberKey key, double value) {
        delegate.number(key, value);
        return this;
    }

    public PiGraphContextBuilder numbers(Map<String, Double> values) {
        delegate.numbers(values);
        return this;
    }

    public PiGraphContextBuilder objects(Map<String, Object> values) {
        delegate.objects(values);
        return this;
    }

    public PiGraphContextBuilder random(DoubleSupplier random) {
        delegate.random(random);
        return this;
    }

    public PiGraphContextBuilder random(RandomSource random) {
        delegate.random(random);
        return this;
    }

    public PiGraphContextBuilder object(String key, Object value) {
        delegate.object(key, value);
        return this;
    }

    public <T> PiGraphContextBuilder object(PiGraphContextKey<T> key, T value) {
        delegate.object(key, value);
        return this;
    }

    public <T> PiGraphContextBuilder object(PiEngineContextKey<T> key, T value) {
        delegate.object(key, value);
        return this;
    }

    public PiGraphContext build() {
        return delegate.build();
    }
}
