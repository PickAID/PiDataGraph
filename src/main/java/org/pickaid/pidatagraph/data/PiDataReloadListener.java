package org.pickaid.pidatagraph.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class PiDataReloadListener<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    private final PiDataRuntimeLoader<T> loader;
    private final Optional<PiDataBuildContext> validationContext;

    protected PiDataReloadListener(PiDataDefinition<T> definition) {
        this(definition, Optional.empty());
    }

    protected PiDataReloadListener(PiDataDefinition<T> definition, PiDataBuildContext validationContext) {
        this(definition, Optional.of(validationContext));
    }

    private PiDataReloadListener(PiDataDefinition<T> definition, Optional<PiDataBuildContext> validationContext) {
        super(GSON, Objects.requireNonNull(definition, "definition").folder());
        this.loader = PiDataRuntimeLoader.of(definition);
        this.validationContext = Objects.requireNonNull(validationContext, "validationContext");
    }

    @Override
    protected final void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        PiDataSet<T> set = validationContext
                .map(context -> loader.loadValidated(files, context))
                .orElseGet(() -> loader.load(files));
        applyData(set, resourceManager, profiler);
    }

    protected abstract void applyData(PiDataSet<T> data, ResourceManager resourceManager, ProfilerFiller profiler);
}
