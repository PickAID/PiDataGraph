package org.pickaid.pidatagraph.data;

import java.util.Objects;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;

public final class PiDataPackRegistries {
    private PiDataPackRegistries() {
    }

    public static void action(
            DataPackRegistryEvent.NewRegistry event,
            ResourceKey<Registry<PiEngineAction>> key,
            PiEngineActionRegistry registry,
            PiDataPackSync sync
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(registry, "registry");
        if (Objects.requireNonNull(sync, "sync") == PiDataPackSync.SYNC_TO_CLIENT) {
            event.dataPackRegistry(key, registry.codec(), registry.codec());
            return;
        }
        event.dataPackRegistry(key, registry.codec());
    }
}
