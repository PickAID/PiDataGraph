package org.pickaid.pidatagraph.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PiDataCatalog<T> {
    private final PiDataDefinition<T> definition;
    private final String namespace;
    private final List<PiDataGenEntry<T>> entries;

    private PiDataCatalog(PiDataDefinition<T> definition, String namespace, List<PiDataGenEntry<T>> entries) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.entries = List.copyOf(entries);
    }

    public static <T> Builder<T> builder(PiDataDefinition<T> definition, String namespace) {
        return new Builder<>(definition, namespace);
    }

    public PiDataSet<T> buildSet() {
        PiDataSet.Builder<T> builder = PiDataSet.builder(definition, namespace);
        entries.forEach(entry -> entry.register(builder));
        return builder.build();
    }

    public static final class Builder<T> {
        private final PiDataDefinition<T> definition;
        private final String namespace;
        private final List<PiDataGenEntry<T>> entries = new ArrayList<>();

        private Builder(PiDataDefinition<T> definition, String namespace) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.namespace = Objects.requireNonNull(namespace, "namespace");
        }

        public Builder<T> add(PiDataGenEntry<T> entry) {
            entries.add(Objects.requireNonNull(entry, "entry"));
            return this;
        }

        public PiDataCatalog<T> build() {
            return new PiDataCatalog<>(definition, namespace, entries);
        }

        public PiDataSet<T> buildSet() {
            return build().buildSet();
        }
    }
}
