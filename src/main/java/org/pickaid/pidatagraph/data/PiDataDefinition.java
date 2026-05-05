package org.pickaid.pidatagraph.data;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class PiDataDefinition<T> {
    private final ResourceLocation id;
    private final String folder;
    private final Codec<T> codec;
    private final List<VerifierEntry<T>> verifiers;

    private PiDataDefinition(ResourceLocation id, String folder, Codec<T> codec, List<VerifierEntry<T>> verifiers) {
        this.id = Objects.requireNonNull(id, "id");
        this.folder = checkFolder(folder);
        this.codec = Objects.requireNonNull(codec, "codec");
        this.verifiers = List.copyOf(verifiers);
    }

    public static <T> Builder<T> builder(ResourceLocation id, String folder, Codec<T> codec) {
        return new Builder<>(id, folder, codec);
    }

    public ResourceLocation id() {
        return id;
    }

    public String folder() {
        return folder;
    }

    public Codec<T> codec() {
        return codec;
    }

    public PiDataValidation verify(PiDataBuildContext context, T value) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(value, "value");
        List<PiDataIssue> issues = new ArrayList<>();
        for (VerifierEntry<T> verifier : verifiers) {
            try {
                verifier.verifier().verify(context, value);
            } catch (PiDataVerificationException error) {
                issues.add(new PiDataIssue(error.path(), issueMessage(error)));
            } catch (RuntimeException error) {
                issues.add(new PiDataIssue(verifier.path(), issueMessage(error)));
            }
        }
        return new PiDataValidation(issues);
    }

    private static String issueMessage(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getName();
        }
        return message;
    }

    private static String checkFolder(String folder) {
        String checked = Objects.requireNonNull(folder, "folder").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("data folder must not be blank");
        }
        if (checked.startsWith("/") || checked.endsWith("/") || checked.contains("..")) {
            throw new IllegalArgumentException("invalid data folder: " + folder);
        }
        return checked;
    }

    public static final class Builder<T> {
        private final ResourceLocation id;
        private final String folder;
        private final Codec<T> codec;
        private final List<VerifierEntry<T>> verifiers = new ArrayList<>();

        private Builder(ResourceLocation id, String folder, Codec<T> codec) {
            this.id = Objects.requireNonNull(id, "id");
            this.folder = checkFolder(folder);
            this.codec = Objects.requireNonNull(codec, "codec");
        }

        public Builder<T> verify(String path, PiDataVerifier<T> verifier) {
            verifiers.add(new VerifierEntry<>(path, verifier));
            return this;
        }

        public PiDataDefinition<T> build() {
            return new PiDataDefinition<>(id, folder, codec, verifiers);
        }
    }

    private record VerifierEntry<T>(String path, PiDataVerifier<T> verifier) {
        private VerifierEntry {
            path = checkVerifierPath(path);
            verifier = Objects.requireNonNull(verifier, "verifier");
        }
    }

    private static String checkVerifierPath(String path) {
        String checked = Objects.requireNonNull(path, "path").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("verifier path must not be blank");
        }
        return checked;
    }
}
