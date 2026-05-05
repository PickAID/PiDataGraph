package org.pickaid.pidatagraph.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PiDataRuntimeLoaderTest {
    @Test
    void loaderDecodesDatapackJsonIntoADataSet() {
        PiDataRuntimeLoader<SpellSpec> loader = PiDataRuntimeLoader.of(spellDefinition());
        Map<ResourceLocation, JsonElement> files = Map.of(
                id("examplemod", "fireball"),
                JsonParser.parseString("{\"name\":\"Fireball\",\"cost\":8}")
        );

        PiDataSet<SpellSpec> set = loader.load(files);

        assertEquals(1, set.entries().size());
        assertEquals(id("examplemod", "fireball"), set.entries().get(0).id());
        assertEquals(new SpellSpec("Fireball", 8), set.entries().get(0).value());
    }

    @Test
    void loaderReportsTheEntryIdWhenDecodeFails() {
        PiDataRuntimeLoader<SpellSpec> loader = PiDataRuntimeLoader.of(spellDefinition());

        PiDataLoadException error = assertThrows(PiDataLoadException.class, () -> loader.load(Map.of(
                id("examplemod", "broken"),
                JsonParser.parseString("{\"name\":\"Broken\"}")
        )));

        assertTrue(error.getMessage().contains("failed to decode examplemod:broken"));
        assertTrue(error.getMessage().contains("cost"));
    }

    @Test
    void loaderCanValidateLoadedDataBeforeApplyingIt() {
        PiDataRuntimeLoader<SpellSpec> loader = PiDataRuntimeLoader.of(spellDefinition());

        PiDataLoadException error = assertThrows(PiDataLoadException.class, () -> loader.loadValidated(
                Map.of(id("examplemod", "negative"), JsonParser.parseString("{\"name\":\"Negative\",\"cost\":-1}")),
                PiDataBuildContext.builder().build()
        ));

        assertEquals("data validation failed: examplemod:negative/cost: cost must be non-negative", error.getMessage());
    }

    private static PiDataDefinition<SpellSpec> spellDefinition() {
        return PiDataDefinition.<SpellSpec>builder(id("pidatagraph", "spell"), "spell", SpellSpec.CODEC)
                .verify("cost", (context, spec) -> {
                    if (spec.cost() < 0) {
                        throw new PiDataVerificationException("cost", "cost must be non-negative");
                    }
                })
                .build();
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    private record SpellSpec(String name, int cost) {
        private static final Codec<SpellSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(SpellSpec::name),
                Codec.INT.fieldOf("cost").forGetter(SpellSpec::cost)
        ).apply(instance, SpellSpec::new));
    }
}
