package org.pickaid.pidatagraph.graphcontext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import net.minecraft.util.RandomSource;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiCompiledBooleanExpression;
import org.pickaid.pidatagraph.expression.PiCompiledDoubleExpression;
import org.pickaid.pidatagraph.expression.PiCompiledIntExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionContext;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

/**
 * Public graph-facing context facade.
 *
 * <p>The P0 runtime still delegates to the existing {@link PiEngineContext}
 * kernel. New application APIs should use this name so PiDataGraph context
 * binding is not confused with the separate PiEngine gameplay module.</p>
 */
public final class PiGraphContext {
    private final PiEngineContext delegate;

    private PiGraphContext(PiEngineContext delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PiGraphContextBuilder graphBuilder() {
        return PiGraphContextBuilder.create();
    }

    public static PiGraphContext fromEngineContext(PiEngineContext context) {
        return new PiGraphContext(context);
    }

    public PiEngineContext asEngineContext() {
        return delegate;
    }

    public PiExpressionContext expressions() {
        return delegate.expressions();
    }

    public PiExpressionLanguage language() {
        return delegate.language();
    }

    public Map<String, Double> numbers() {
        return delegate.numbers();
    }

    public double number(String key) {
        return delegate.number(key);
    }

    public double number(PiEngineNumberKey key) {
        return delegate.number(key);
    }

    public boolean hasNumber(String key) {
        return delegate.hasNumber(key);
    }

    public boolean hasNumber(PiEngineNumberKey key) {
        return delegate.hasNumber(key);
    }

    public PiExpressionScope expressionScope() {
        return delegate.expressionScope();
    }

    public Builder derive() {
        return new Builder(delegate.derive());
    }

    public double evaluate(PiCompiledDoubleExpression expression) {
        return delegate.evaluate(expression);
    }

    public int evaluate(PiCompiledIntExpression expression) {
        return delegate.evaluate(expression);
    }

    public boolean evaluate(PiCompiledBooleanExpression expression) {
        return delegate.evaluate(expression);
    }

    public double evaluate(PiDoubleExpression expression) {
        return delegate.evaluate(expression);
    }

    public int evaluate(PiIntExpression expression) {
        return delegate.evaluate(expression);
    }

    public boolean evaluate(PiBooleanExpression expression) {
        return delegate.evaluate(expression);
    }

    public Optional<Object> object(String key) {
        return delegate.object(key);
    }

    public boolean hasObject(String key) {
        return delegate.hasObject(key);
    }

    public <T> Optional<T> object(String key, Class<T> type) {
        return delegate.object(key, type);
    }

    public <T> Optional<T> object(PiGraphContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return object(key.name(), key.type());
    }

    public <T> boolean hasObject(PiGraphContextKey<T> key) {
        return hasObject(Objects.requireNonNull(key, "key").name());
    }

    public static final class Builder {
        private final PiEngineContext.Builder delegate;

        private Builder() {
            this(PiEngineContext.builder());
        }

        private Builder(PiEngineContext.Builder delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public Builder language(PiExpressionLanguage language) {
            delegate.language(language);
            return this;
        }

        public Builder number(String key, double value) {
            delegate.number(key, value);
            return this;
        }

        public Builder number(PiEngineNumberKey key, double value) {
            delegate.number(key, value);
            return this;
        }

        public Builder numbers(Map<String, Double> values) {
            delegate.numbers(values);
            return this;
        }

        public Builder objects(Map<String, Object> values) {
            delegate.objects(values);
            return this;
        }

        public Builder random(DoubleSupplier random) {
            delegate.random(random);
            return this;
        }

        public Builder random(RandomSource random) {
            delegate.random(random);
            return this;
        }

        public Builder object(String key, Object value) {
            delegate.object(key, value);
            return this;
        }

        public <T> Builder object(PiGraphContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            return object(PiEngineContextKey.of(key.name(), key.type()), value);
        }

        public <T> Builder object(PiEngineContextKey<T> key, T value) {
            delegate.object(key, value);
            return this;
        }

        public PiGraphContext build() {
            return new PiGraphContext(delegate.build());
        }
    }
}
