package org.pickaid.pidatagraph.data;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public final class PiDataProvider implements DataProvider {
    private final PackOutput output;
    private final String name;
    private final Optional<PiDataBuildContext> validationContext;
    private final List<PiDataSet<?>> sets;

    public PiDataProvider(PackOutput output, String name, PiDataSet<?>... sets) {
        this(output, name, Optional.empty(), Arrays.asList(sets));
    }

    public PiDataProvider(PackOutput output, String name, PiDataBuildContext validationContext, PiDataSet<?>... sets) {
        this(output, name, Optional.of(validationContext), Arrays.asList(sets));
    }

    public PiDataProvider(PackOutput output, String name, List<PiDataSet<?>> sets) {
        this(output, name, Optional.empty(), sets);
    }

    public PiDataProvider(PackOutput output, String name, PiDataBuildContext validationContext, List<PiDataSet<?>> sets) {
        this(output, name, Optional.of(validationContext), sets);
    }

    private PiDataProvider(PackOutput output, String name, Optional<PiDataBuildContext> validationContext, List<PiDataSet<?>> sets) {
        this.output = Objects.requireNonNull(output, "output");
        this.name = Objects.requireNonNull(name, "name");
        this.validationContext = Objects.requireNonNull(validationContext, "validationContext");
        this.sets = List.copyOf(Objects.requireNonNull(sets, "sets"));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        validationContext.ifPresent(context -> {
            for (PiDataSet<?> set : sets) {
                PiDataValidation validation = set.verify(context);
                if (!validation.ok()) {
                    PiDataIssue issue = validation.issues().get(0);
                    throw new IllegalStateException("data validation failed: " + issue.path() + ": " + issue.message());
                }
            }
        });
        List<PiDataJsonFile> files = generatedFiles();
        Path root = output.getOutputFolder();
        return CompletableFuture.allOf(files.stream()
                .map(file -> DataProvider.saveStable(cachedOutput, file.json(), root.resolve(file.path())))
                .toArray(CompletableFuture[]::new));
    }

    private List<PiDataJsonFile> generatedFiles() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        return sets.stream()
                .flatMap(set -> set.encode().stream())
                .peek(file -> {
                    if (!paths.add(file.path())) {
                        throw new IllegalStateException("duplicate generated data file: " + file.path());
                    }
                })
                .toList();
    }

    @Override
    public String getName() {
        return name;
    }
}
