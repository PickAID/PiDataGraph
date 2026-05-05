package org.pickaid.pidatagraph.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class PiDataRuntimeLoader<T> {
    private final PiDataDefinition<T> definition;

    private PiDataRuntimeLoader(PiDataDefinition<T> definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public static <T> PiDataRuntimeLoader<T> of(PiDataDefinition<T> definition) {
        return new PiDataRuntimeLoader<>(definition);
    }

    public PiDataDefinition<T> definition() {
        return definition;
    }

    public PiDataSet<T> load(Map<ResourceLocation, JsonElement> files) {
        Objects.requireNonNull(files, "files");
        PiDataSet.Builder<T> builder = PiDataSet.builder(definition, definition.id().getNamespace());
        files.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> builder.entry(entry.getKey(), decode(entry.getKey(), entry.getValue())));
        return builder.build();
    }

    public PiDataSet<T> loadValidated(Map<ResourceLocation, JsonElement> files, PiDataBuildContext context) {
        PiDataSet<T> set = load(files);
        PiDataValidation validation = set.verify(Objects.requireNonNull(context, "context"));
        if (!validation.ok()) {
            PiDataIssue issue = validation.issues().get(0);
            throw new PiDataLoadException("data validation failed: " + issue.path() + ": " + issue.message());
        }
        return set;
    }

    private T decode(ResourceLocation id, JsonElement json) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(json, "json");
        DataResult<T> result = definition.codec().parse(JsonOps.INSTANCE, json);
        return result.resultOrPartial(message -> {
        }).orElseThrow(() -> new PiDataLoadException("failed to decode " + id + ": "
                + result.error().map(DataResult.PartialResult::message).orElse("unknown error")));
    }
}
