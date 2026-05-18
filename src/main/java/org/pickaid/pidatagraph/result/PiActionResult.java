package org.pickaid.pidatagraph.result;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;

public record PiActionResult(ResourceLocation type, String payloadKey, Kind kind) implements PiGraphResult {
    public PiActionResult {
        type = Objects.requireNonNull(type, "type");
        payloadKey = PiEngineKeyNames.frameValue(payloadKey);
        kind = Objects.requireNonNull(kind, "kind");
    }

    public static PiActionResult request(ResourceLocation type, String payloadKey) {
        return new PiActionResult(type, payloadKey, Kind.REQUEST);
    }

    public enum Kind {
        REQUEST
    }
}
