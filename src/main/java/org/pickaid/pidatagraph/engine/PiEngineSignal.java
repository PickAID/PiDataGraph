package org.pickaid.pidatagraph.engine;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;

public record PiEngineSignal(ResourceLocation type, PiEngineContext context) {
    public PiEngineSignal {
        type = Objects.requireNonNull(type, "type");
        context = Objects.requireNonNull(context, "context");
    }

    public static Builder builder(ResourceLocation type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final ResourceLocation type;
        private final PiEngineContext.Builder context = PiEngineContext.builder();

        private Builder(ResourceLocation type) {
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder number(String key, double value) {
            context.number(key, value);
            return this;
        }

        public Builder number(PiEngineNumberKey key, double value) {
            context.number(key, value);
            return this;
        }

        public Builder random(DoubleSupplier random) {
            context.random(random);
            return this;
        }

        public Builder random(RandomSource random) {
            context.random(random);
            return this;
        }

        public Builder object(String key, Object value) {
            context.object(key, value);
            return this;
        }

        public <T> Builder object(PiEngineContextKey<T> key, T value) {
            context.object(key, value);
            return this;
        }

        public Builder configureContext(Consumer<PiEngineContext.Builder> consumer) {
            Objects.requireNonNull(consumer, "consumer").accept(context);
            return this;
        }

        public PiEngineSignal build() {
            return new PiEngineSignal(type, context.build());
        }
    }
}
