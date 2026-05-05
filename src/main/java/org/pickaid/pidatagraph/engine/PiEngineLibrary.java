package org.pickaid.pidatagraph.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.data.PiDataEntry;
import org.pickaid.pidatagraph.data.PiDataIssue;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.data.PiDataValidation;

public final class PiEngineLibrary<T> {
    private final Map<ResourceLocation, T> entries;

    private PiEngineLibrary(Map<ResourceLocation, T> entries) {
        this.entries = Map.copyOf(entries);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <S, R> PiEngineLibrary<R> compile(
            PiDataSet<S> data,
            PiEngineBuildContext context,
            PiEngineContentType<S, R> type
    ) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(type, "type");
        PiEngineBuildContext scopedContext = context.withScope(type.scope());
        PiDataValidation validation = data.verify(scopedContext.dataContext());
        if (!validation.ok()) {
            PiDataIssue issue = validation.issues().get(0);
            throw new IllegalStateException("engine data validation failed: " + issue.path() + ": " + issue.message());
        }

        Builder<R> builder = builder();
        for (PiDataEntry<S> entry : data.entries()) {
            try {
                R compiled = Objects.requireNonNull(type.compile(entry, scopedContext), "compiler returned null");
                builder.entry(entry.id(), compiled);
            } catch (RuntimeException error) {
                throw new IllegalStateException("engine content compile failed: " + entry.id() + ": " + issueMessage(error), error);
            }
        }
        return builder.build();
    }

    public Optional<T> get(ResourceLocation id) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(id, "id")));
    }

    public T require(ResourceLocation id) {
        T value = entries.get(Objects.requireNonNull(id, "id"));
        if (value == null) {
            throw new IllegalArgumentException("missing engine content: " + id);
        }
        return value;
    }

    public Map<ResourceLocation, T> entries() {
        return entries;
    }

    public static final class Builder<T> {
        private final LinkedHashMap<ResourceLocation, T> entries = new LinkedHashMap<>();

        public Builder<T> entry(ResourceLocation id, T value) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(value, "value");
            if (entries.putIfAbsent(id, value) != null) {
                throw new IllegalArgumentException("duplicate engine content: " + id);
            }
            return this;
        }

        public PiEngineLibrary<T> build() {
            return new PiEngineLibrary<>(entries);
        }
    }

    private static String issueMessage(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getName();
        }
        return message;
    }
}
