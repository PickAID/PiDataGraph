package org.pickaid.pidatagraph.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

class PiDataSetTest {
    @TempDir
    private java.nio.file.Path tempDir;

    @Test
    void buildsDatapackJsonFilesFromCodecBackedEntries() {
        PiDataDefinition<SpellSpec> spells = spellDefinition();
        PiDataSet<SpellSpec> set = PiDataSet.builder(spells, "examplemod")
                .entry("fireball", new SpellSpec(PiDoubleExpression.of("base + level * 2"), PiIntExpression.of("20 - haste")))
                .entry("ice_lance", new SpellSpec(PiDoubleExpression.of("level * 1.5"), PiIntExpression.of("12")))
                .build();

        List<PiDataJsonFile> files = set.encode();

        assertEquals("data/examplemod/spell/fireball.json", files.get(0).path());
        JsonObject fireball = files.get(0).json().getAsJsonObject();
        assertEquals("base + level * 2", fireball.get("damage").getAsString());
        assertEquals("20 - haste", fireball.get("cooldown").getAsString());
        assertEquals("data/examplemod/spell/ice_lance.json", files.get(1).path());
    }

    @Test
    void providerWritesGeneratedFilesIntoMinecraftDatagenOutput() throws Exception {
        PiDataSet<SpellSpec> set = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("fireball", new SpellSpec(PiDoubleExpression.of("base + level * 2"), PiIntExpression.of("20 - haste")))
                .build();
        PiDataProvider provider = new PiDataProvider(new PackOutput(tempDir), "Example Spell Data", set);

        provider.run(CachedOutput.NO_CACHE).join();

        java.nio.file.Path file = tempDir.resolve("data/examplemod/spell/fireball.json");
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.readString(file).contains("\"damage\": \"base + level * 2\""));
    }


    @Test
    void rejectsDuplicateGeneratedIds() {
        PiDataSet.Builder<SpellSpec> builder = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("fireball", new SpellSpec(PiDoubleExpression.of("1"), PiIntExpression.of("20")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                builder.entry("fireball", new SpellSpec(PiDoubleExpression.of("2"), PiIntExpression.of("10"))));

        assertEquals("duplicate data entry: examplemod:fireball", error.getMessage());
    }

    @Test
    void definitionVerificationCanValidateEmbeddedExpressions() {
        PiDataDefinition<SpellSpec> spells = spellDefinition();
        PiDataBuildContext context = PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base", "level", "haste"))
                .build();

        PiDataValidation validation = spells.verify(context,
                new SpellSpec(PiDoubleExpression.of("base + level * 2"), PiIntExpression.of("20 - haste")));

        assertTrue(validation.ok());
    }

    @Test
    void definitionVerificationReportsBadEmbeddedExpressionsWithPath() {
        PiDataDefinition<SpellSpec> spells = spellDefinition();
        PiDataBuildContext context = PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base", "level"))
                .build();

        PiDataValidation validation = spells.verify(context,
                new SpellSpec(PiDoubleExpression.of("base + missing"), PiIntExpression.of("20")));

        assertEquals(1, validation.issues().size());
        assertEquals("damage", validation.issues().get(0).path());
        assertEquals("invalid expression `base + missing` for variables [base, level]", validation.issues().get(0).message());
    }

    @Test
    void definitionVerificationKeepsPathWhenVerifierThrowsWithoutMessage() {
        PiDataDefinition<SpellSpec> spells = PiDataDefinition.<SpellSpec>builder(id("spell"), "spell", SpellSpec.CODEC)
                .verify("damage", (context, spec) -> {
                    throw new NullPointerException();
                })
                .build();

        PiDataValidation validation = spells.verify(PiDataBuildContext.builder().build(),
                new SpellSpec(PiDoubleExpression.of("1"), PiIntExpression.of("20")));

        assertEquals(1, validation.issues().size());
        assertEquals("damage", validation.issues().get(0).path());
        assertEquals(NullPointerException.class.getName(), validation.issues().get(0).message());
    }

    @Test
    void definitionRejectsBlankVerifierPaths() {
        PiDataDefinition.Builder<SpellSpec> builder = PiDataDefinition.builder(id("spell"), "spell", SpellSpec.CODEC);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                builder.verify(" ", (context, spec) -> {
                }));

        assertEquals("verifier path must not be blank", error.getMessage());
    }

    @Test
    void dataSetVerificationIncludesEntryIdInIssuePath() {
        PiDataSet<SpellSpec> set = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("fireball", new SpellSpec(PiDoubleExpression.of("base + missing"), PiIntExpression.of("20")))
                .build();
        PiDataBuildContext context = PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base", "level"))
                .build();

        PiDataValidation validation = set.verify(context);

        assertEquals(1, validation.issues().size());
        assertEquals("examplemod:fireball/damage", validation.issues().get(0).path());
    }

    @Test
    void catalogLetsLargeDatagenSplitEntriesAcrossClasses() {
        PiDataSet<SpellSpec> set = PiDataCatalog.builder(spellDefinition(), "examplemod")
                .add(new FireSpellEntries())
                .add(builder -> builder.entry("ice_lance",
                        new SpellSpec(PiDoubleExpression.of("level * 1.5"), PiIntExpression.of("12"))))
                .buildSet();

        assertEquals("examplemod:fireball", set.entries().get(0).id().toString());
        assertEquals("examplemod:flame_wall", set.entries().get(1).id().toString());
        assertEquals("examplemod:ice_lance", set.entries().get(2).id().toString());
    }

    @Test
    void providerFailsBeforeWritingInvalidGeneratedData() {
        PiDataSet<SpellSpec> set = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("fireball", new SpellSpec(PiDoubleExpression.of("base + missing"), PiIntExpression.of("20")))
                .build();
        PiDataBuildContext context = PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base", "level"))
                .build();
        PiDataProvider provider = new PiDataProvider(new PackOutput(tempDir), "Example Spell Data", context, set);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                provider.run(CachedOutput.NO_CACHE));

        assertEquals("data validation failed: examplemod:fireball/damage: invalid expression `base + missing` for variables [base, level]",
                error.getMessage());
    }

    private static PiDataDefinition<SpellSpec> spellDefinition() {
        return PiDataDefinition.<SpellSpec>builder(id("spell"), "spell", SpellSpec.CODEC)
                .verify("damage", (context, spec) -> spec.damage().compile(context.expressionLanguage(), context.expressionScope()))
                .verify("cooldown", (context, spec) -> spec.cooldown().compile(context.expressionLanguage(), context.expressionScope()))
                .build();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }

    private static final class FireSpellEntries implements PiDataGenEntry<SpellSpec> {
        @Override
        public void register(PiDataSet.Builder<SpellSpec> builder) {
            builder.entry("fireball", new SpellSpec(PiDoubleExpression.of("base + level * 2"), PiIntExpression.of("20 - haste")));
            builder.entry("flame_wall", new SpellSpec(PiDoubleExpression.of("base + 3"), PiIntExpression.of("30")));
        }
    }

    private record SpellSpec(PiDoubleExpression damage, PiIntExpression cooldown) {
        private static final Codec<SpellSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PiDoubleExpression.CODEC.fieldOf("damage").forGetter(SpellSpec::damage),
                PiIntExpression.CODEC.fieldOf("cooldown").forGetter(SpellSpec::cooldown)
        ).apply(instance, SpellSpec::new));
    }
}
