package org.pickaid.pidatagraph.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class PiDataSet<T> {
    private final PiDataDefinition<T> definition;
    private final List<PiDataEntry<T>> entries;
    private final Map<ResourceLocation, T> valuesById;

    private PiDataSet(PiDataDefinition<T> definition, List<PiDataEntry<T>> entries) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.entries = List.copyOf(entries);
        LinkedHashMap<ResourceLocation, T> values = new LinkedHashMap<>();
        for (PiDataEntry<T> entry : entries) {
            values.put(entry.id(), entry.value());
        }
        this.valuesById = Map.copyOf(values);
    }

    public static <T> Builder<T> builder(PiDataDefinition<T> definition, String namespace) {
        return new Builder<>(definition, namespace);
    }

    public PiDataDefinition<T> definition() {
        return definition;
    }

    public List<PiDataEntry<T>> entries() {
        return entries;
    }

    public Optional<T> value(ResourceLocation id) {
        return Optional.ofNullable(valuesById.get(Objects.requireNonNull(id, "id")));
    }

    public Optional<T> value(ResourceKey<?> key) {
        return value(Objects.requireNonNull(key, "key").location());
    }

    public PiDataValidation verify(PiDataBuildContext context) {
        List<PiDataIssue> issues = new ArrayList<>();
        for (PiDataEntry<T> entry : entries) {
            PiDataValidation validation = definition.verify(context, entry.value());
            for (PiDataIssue issue : validation.issues()) {
                issues.add(new PiDataIssue(entry.id() + "/" + issue.path(), issue.message()));
            }
        }
        return new PiDataValidation(issues);
    }

    public List<PiDataJsonFile> encode() {
        List<PiDataJsonFile> files = new ArrayList<>();
        for (PiDataEntry<T> entry : entries) {
            JsonElement json = definition.codec().encodeStart(JsonOps.INSTANCE, entry.value()).getOrThrow(false, message -> {
                throw new IllegalStateException("failed to encode " + entry.id() + ": " + message);
            });
            files.add(new PiDataJsonFile(pathFor(entry.id()), json));
        }
        return List.copyOf(files);
    }

    private String pathFor(ResourceLocation id) {
        return "data/" + id.getNamespace() + "/" + definition.folder() + "/" + id.getPath() + ".json";
    }

    public static final class Builder<T> {
        private final PiDataDefinition<T> definition;
        private final String namespace;
        private final Map<ResourceLocation, PiDataEntry<T>> entries = new LinkedHashMap<>();

        private Builder(PiDataDefinition<T> definition, String namespace) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.namespace = checkNamespace(namespace);
        }

        public Builder<T> entry(String path, T value) {
            return entry(new ResourceLocation(namespace, checkEntryPath(path)), value);
        }

        public Builder<T> entry(ResourceLocation id, T value) {
            ResourceLocation checkedId = Objects.requireNonNull(id, "id");
            checkEntryPath(checkedId.getPath());
            PiDataEntry<T> entry = new PiDataEntry<>(checkedId, value);
            if (entries.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalArgumentException("duplicate data entry: " + entry.id());
            }
            return this;
        }

        public Builder<T> entry(ResourceKey<?> key, T value) {
            return entry(Objects.requireNonNull(key, "key").location(), value);
        }

        public PiDataSet<T> build() {
            return new PiDataSet<>(definition, new ArrayList<>(entries.values()));
        }

        static String checkNamespace(String namespace) {
            String checked = Objects.requireNonNull(namespace, "namespace").trim();
            if (checked.isEmpty()) {
                throw new IllegalArgumentException("namespace must not be blank");
            }
            if (!isValidNamespace(checked)) {
                throw new IllegalArgumentException("invalid namespace: " + namespace);
            }
            return checked;
        }

        private static String checkEntryPath(String path) {
            String checked = Objects.requireNonNull(path, "path").trim();
            if (checked.isEmpty()) {
                throw new IllegalArgumentException("data entry path must not be blank");
            }
            if (checked.startsWith("/") || checked.endsWith("/") || checked.contains("..") || !isValidPath(checked)) {
                throw new IllegalArgumentException("invalid data entry path: " + path);
            }
            return checked;
        }

        private static boolean isValidNamespace(String namespace) {
            for (int index = 0; index < namespace.length(); index++) {
                char next = namespace.charAt(index);
                if (!(next >= 'a' && next <= 'z')
                        && !(next >= '0' && next <= '9')
                        && next != '_' && next != '-' && next != '.') {
                    return false;
                }
            }
            return true;
        }

        private static boolean isValidPath(String path) {
            for (int index = 0; index < path.length(); index++) {
                char next = path.charAt(index);
                if (!(next >= 'a' && next <= 'z')
                        && !(next >= '0' && next <= '9')
                        && next != '_' && next != '-' && next != '.' && next != '/') {
                    return false;
                }
            }
            return true;
        }
    }
}
