package org.pickaid.pidatagraph.engine;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Unit;
import org.pickaid.pidatagraph.data.PiDataEntry;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiEngineRunner<T> {
    private final ResourceKey<Registry<PiEngineAction>> registryKey;
    private final PiEngineContextBinder<T> binder;

    private PiEngineRunner(ResourceKey<Registry<PiEngineAction>> registryKey, PiEngineContextBinder<T> binder) {
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
        return new PiEngineRunner<>(Objects.requireNonNull(registryKey, "registryKey"), binder);
    }

    public PiEngineContextBinder<T> binder() {
        return binder;
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

    public <R> R run(
            PiDataSet<PiEngineAction> actions,
            ResourceLocation id,
            T input,
            Function<? super PiEngineFrame, ? extends R> resultMapper
    ) {
        return Objects.requireNonNull(resultMapper, "resultMapper").apply(run(actions, id, input));
    }

    public PiEngineFrame run(RegistryAccess access, ResourceLocation id, T input) {
        Registry<PiEngineAction> registry = registry(access);
        PiEngineAction action = registry.get(Objects.requireNonNull(id, "id"));
        if (action == null) {
            throw new IllegalArgumentException("missing engine action " + id + " in registry " + registryKey.location());
        }
        return run(action, input);
    }

    public <R> R run(
            RegistryAccess access,
            ResourceLocation id,
            T input,
            Function<? super PiEngineFrame, ? extends R> resultMapper
    ) {
        return Objects.requireNonNull(resultMapper, "resultMapper").apply(run(access, id, input));
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
        requireRegistryKey();
        return access.registryOrThrow(registryKey);
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

    private void verify(ResourceLocation id, PiEngineAction action) {
        PiEngineActionType<?> type = actionType(action);
        actionContract(action, type);
        try {
            action.verify(binder.validationContext(), id.toString());
        } catch (PiDataVerificationException error) {
            throw new PiDataVerificationException(withEntryPath(id, error.path()), error.getMessage());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(id.toString(), issueMessage(error));
        }
    }

    private PiEngineAction find(PiDataSet<PiEngineAction> actions, ResourceLocation id) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(id, "id");
        for (PiDataEntry<PiEngineAction> entry : actions.entries()) {
            if (entry.id().equals(id)) {
                return entry.value();
            }
        }
        throw new IllegalArgumentException("missing engine action " + id + " in data set " + actions.definition().id());
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
