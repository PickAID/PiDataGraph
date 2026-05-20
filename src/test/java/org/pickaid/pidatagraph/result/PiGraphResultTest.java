package org.pickaid.pidatagraph.result;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class PiGraphResultTest {
    @Test
    void actionResultCarriesTypeAndPayloadKey() {
        PiActionResult result = PiActionResult.request(id("pidamage:damage_request"), "impact_damage");

        assertEquals(id("pidamage:damage_request"), result.type());
        assertEquals("impact_damage", result.payloadKey());
    }

    @Test
    void p0RuntimeHelpersReturnOwnerModuleRequestsWithoutApplyingThem() {
        PiActionResult capture = PiGraphResults.captureRequest("capture.target");
        PiActionResult throwTarget = PiGraphResults.throwRequest("capture.throw");
        PiActionResult damage = PiGraphResults.damageRequest("impact_damage");
        PiPresentationResult cue = PiGraphResults.cue("beam");

        assertEquals(id("piengine:capture"), capture.type());
        assertEquals("capture.target", capture.payloadKey());
        assertEquals(id("piengine:throw"), throwTarget.type());
        assertEquals("capture.throw", throwTarget.payloadKey());
        assertEquals(id("pidamage:damage_request"), damage.type());
        assertEquals("impact_damage", damage.payloadKey());
        assertEquals(id("pirender:cue"), cue.type());
        assertEquals("beam", cue.payloadKey());
    }

    @Test
    void debugFrameCarriesStructuredResultsWithoutApplyingThem() {
        PiActionResult damage = PiActionResult.request(id("pidamage:damage_request"), "impact_damage");

        PiDebugFrame frame = new PiDebugFrame(
                id("example:spell_cast"),
                "impact",
                List.of(damage),
                Map.of("accepted", true)
        );

        assertEquals(id("example:spell_cast"), frame.graphId());
        assertEquals(damage, frame.results().get(0));
        assertEquals(true, frame.values().get("accepted"));
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
