package org.pickaid.pidatagraph.engine;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Unit;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataEntry;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiEngineRunner<T> {
    private final Supplier<ResourceKey<Registry<PiEngineAction>>> registryKey;
    private final PiEngineContextBinder<T> binder;

    private PiEngineRunner(Supplier<ResourceKey<Registry<PiEngineAction>>> registryKey, PiEngineContextBinder<T> binder) {
        this.registryKey = registryKey;
        this.binder = Objects.requireNonNull(binder, "binder");
    }

    public static <T> PiEngineRunner<T> actionRegistry(PiEngineContextBinder<T> binder) {
        return new PiEngineRunner<>(null, binder);
    }

    public static <T> PiEngineRunner<T> actionRegistry(
            ResourceKey<Registry<PiEngineAction>> registryKey,
            PiEngineContextBinder<T> binder
    ) {
        Objects.requireNonNull(registryKey, "registryKey");
        return new PiEngineRunner<>(() -> registryKey, binder);
    }

    public static <T> PiEngineRunner<T> actionRegistry(
            Supplier<ResourceKey<Registry<PiEngineAction>>> registryKey,
            PiEngineContextBinder<T> binder
    ) {
        return new PiEngineRunner<>(Objects.requireNonNull(registryKey, "registryKey"), binder);
    }

    public PiEngineContextBinder<T> binder() {
        return binder;
    }

    public PiDataBuildContext validationContext() {
        return binder.validationContext();
    }

    public PiEngineFrame run(PiEngineAction action, T input) {
        Objects.requireNonNull(action, "action");
        PiEngineActionType<?> type = actionType(action);
        PiEngineContext context = Objects.requireNonNull(binder.bind(input), "binder returned null context");
        context.verifyContract(binderContract(), "binder " + binder.getClass().getName());
        context.verifyContract(actionContract(action, type), "action " + type.id());
        return Objects.requireNonNull(action.execute(context), "action " + type.id() + " returned null frame");
    }

    public <R> R run(PiEngineAction action, T input, Function<? super PiEngineFrame, ? extends R> resultMapper) {
        return Objects.requireNonNull(resultMapper, "resultMapper").apply(run(action, input));
    }

    public PiEngineFrame run(PiDataSet<PiEngineAction> actions, ResourceLocation id, T input) {
        return run(find(actions, id), input);
    }

    public PiEngineFrame run(PiDataSet<PiEngineAction> actions, ResourceKey<?> key, T input) {
        return run(actions, Objects.requireNonNull(key, "key").location(), input);
    }

    public <R> R run(
            PiDataSet<PiEngineAction> actions,
            ResourceLocation id,
            T input,
            Function<? super PiEngineFrame, ? extends R> resultMapper
    ) {
        return Objects.requireNonNull(resultMapper, "resultMapper").apply(run(actions, id, input));
    }

    public <R> R run(
            PiDataSet<PiEngineAction> actions,
            ResourceKey<?> key,
            T input,
            Function<? super PiEngineFrame, ? extends R> resultMapper
    ) {
        return run(actions, Objects.requireNonNull(key, "key").location(), input, resultMapper);
    }

    public PiEngineFrame run(RegistryAccess access, ResourceLocation id, T input) {
        Registry<PiEngineAction> registry = registry(access);
        PiEngineAction action = registry.get(Objects.requireNonNull(id, "id"));
        if (action == null) {
            throw new IllegalArgumentException("missing engine action " + id + " in registry " + registryKey().location());
        }
        return run(action, input);
    }

    public PiEngineFrame run(RegistryAccess access, ResourceKey<?> key, T input) {
        return run(access, Objects.requireNonNull(key, "key").location(), input);
    }

    public <R> R run(
            RegistryAccess access,
            ResourceLocation id,
            T input,
            Function<? super PiEngineFrame, ? extends R> resultMapper
    ) {
        return Objects.requireNonNull(resultMapper, "resultMapper").apply(run(access, id, input));
    }

    public <R> R run(
            RegistryAccess access,
            ResourceKey<?> key,
            T input,
            Function<? super PiEngineFrame, ? extends R> resultMapper
    ) {
        return run(access, Objects.requireNonNull(key, "key").location(), input, resultMapper);
    }

    public void verifyAll(PiDataSet<PiEngineAction> actions) {
        Objects.requireNonNull(actions, "actions");
        for (PiDataEntry<PiEngineAction> entry : actions.entries()) {
            verify(entry.id(), entry.value());
        }
    }

    public void verifyAll(RegistryAccess access) {
        Registry<PiEngineAction> registry = registry(access);
        for (Map.Entry<ResourceKey<PiEngineAction>, PiEngineAction> entry : registry.entrySet()) {
            verify(entry.getKey().location(), entry.getValue());
        }
    }

    public PreparableReloadListener reloadVerifier(RegistryAccess access) {
        Objects.requireNonNull(access, "access");
        requireRegistryKey();
        return (barrier, resourceManager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                barrier.wait(Unit.INSTANCE).thenRunAsync(() -> verifyAll(access), gameExecutor);
    }

    private Registry<PiEngineAction> registry(RegistryAccess access) {
        Objects.requireNonNull(access, "access");
        return access.registryOrThrow(registryKey());
    }

    private PiEngineContextContract binderContract() {
        return Objects.requireNonNull(binder.contract(), "binder returned null contract");
    }

    private static PiEngineActionType<?> actionType(PiEngineAction action) {
        return Objects.requireNonNull(action.type(), "action returned null type");
    }

    private static PiEngineContextContract actionContract(PiEngineAction action, PiEngineActionType<?> type) {
        return Objects.requireNonNull(action.contextContract(), "action " + type.id() + " returned null context contract");
    }

    private void requireRegistryKey() {
        if (registryKey == null) {
            throw new IllegalStateException("this engine runner was created without a datapack registry key");
        }
    }

    private ResourceKey<Registry<PiEngineAction>> registryKey() {
        requireRegistryKey();
        return Objects.requireNonNull(registryKey.get(), "registryKey supplier returned null");
    }

    private void verify(ResourceLocation id, PiEngineAction action) {
        PiEngineActionType<?> type = actionType(action);
        PiEngineContextContract contract = actionContract(action, type);
        PiDataBuildContext context = binder.validationContext();
        try {
            contract.verify(context, id + ".context");
            action.verify(context, id.toString());
        } catch (PiDataVerificationException error) {
            throw new PiDataVerificationException(withEntryPath(id, error.path()), error.getMessage());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(id.toString(), issueMessage(error));
        }
    }

    private PiEngineAction find(PiDataSet<PiEngineAction> actions, ResourceLocation id) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(id, "id");
        return actions.value(id).orElseThrow(() ->
                new IllegalArgumentException("missing engine action " + id + " in data set " + actions.definition().id()));
    }

    private static String withEntryPath(ResourceLocation id, String path) {
        String prefix = id.toString();
        if (path.equals(prefix)) {
            return prefix;
        }
        if (path.startsWith(prefix + ".")) {
            return prefix + "/" + path.substring(prefix.length() + 1);
        }
        return prefix + "/" + path;
    }

    private static String issueMessage(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getName();
        }
        return message;
    }
}
