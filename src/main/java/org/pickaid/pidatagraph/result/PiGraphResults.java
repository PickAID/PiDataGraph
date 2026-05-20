package org.pickaid.pidatagraph.result;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class PiGraphResults {
    public static final ResourceLocation CAPTURE = id("piengine", "capture");
    public static final ResourceLocation THROW = id("piengine", "throw");
    public static final ResourceLocation DAMAGE_REQUEST = id("pidamage", "damage_request");
    public static final ResourceLocation CUE = id("pirender", "cue");

    private PiGraphResults() {
    }

    public static PiActionResult captureRequest(String payloadKey) {
        return PiActionResult.request(CAPTURE, payloadKey);
    }

    public static PiActionResult throwRequest(String payloadKey) {
        return PiActionResult.request(THROW, payloadKey);
    }

    public static PiActionResult damageRequest(String payloadKey) {
        return PiActionResult.request(DAMAGE_REQUEST, payloadKey);
    }

    public static PiPresentationResult cue(String payloadKey) {
        return PiPresentationResult.cue(CUE, payloadKey);
    }

    private static ResourceLocation id(String namespace, String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse(namespace + ":" + path));
    }
}
