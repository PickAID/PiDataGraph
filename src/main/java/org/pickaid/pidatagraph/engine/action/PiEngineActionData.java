package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.data.PiDataDefinition;
import org.pickaid.pidatagraph.engine.PiEngineContentType;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

public final class PiEngineActionData {
    private PiEngineActionData() {
    }

    public static PiDataDefinition<PiEngineAction> definition(
            ResourceLocation id,
            String folder,
            PiEngineActionRegistry registry
    ) {
        return definition(id, folder, Objects.requireNonNull(registry, "registry").codec());
    }

    public static PiDataDefinition<PiEngineAction> definition(
            ResourceLocation id,
            String folder,
            Codec<PiEngineAction> codec
    ) {
        return PiDataDefinition.<PiEngineAction>builder(id, folder, codec)
                .verify("root", (context, action) -> action.verify(context, "root"))
                .build();
    }

    public static PiEngineContentType<PiEngineAction, PiEngineAction> contentType(
            PiDataDefinition<PiEngineAction> definition,
            PiExpressionScope scope
    ) {
        return PiEngineContentType.builder(
                        Objects.requireNonNull(definition, "definition"),
                        (entry, context) -> entry.value())
                .scope(Objects.requireNonNull(scope, "scope"))
                .build();
    }
}
