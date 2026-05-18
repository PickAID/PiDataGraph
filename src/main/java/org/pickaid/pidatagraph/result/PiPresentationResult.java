package org.pickaid.pidatagraph.result;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;

public record PiPresentationResult(ResourceLocation type, String payloadKey) implements PiGraphResult {
    public PiPresentationResult {
        type = Objects.requireNonNull(type, "type");
        payloadKey = PiEngineKeyNames.frameValue(payloadKey);
    }

    public static PiPresentationResult cue(ResourceLocation type, String payloadKey) {
        return new PiPresentationResult(type, payloadKey);
    }
}
