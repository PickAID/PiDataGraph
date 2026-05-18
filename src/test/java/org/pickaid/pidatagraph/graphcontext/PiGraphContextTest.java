package org.pickaid.pidatagraph.graphcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.PiEngineRunner;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

final class PiGraphContextTest {
    @Test
    void graphContextSchemaOwnsPublicName() {
        PiGraphContextSchema schema = PiGraphContextSchema.builder("spell_cast")
                .object("caster", Object.class)
                .number("base_damage")
                .build();

        assertEquals("spell_cast", schema.id());
        assertEquals(2, schema.fields().size());
    }

    @Test
    void recordBinderDeclaresSchemaAndBuildsContext() {
        PiGraphContextBinder<SpellCastInput> binder = PiGraphContextBindings.record("spell_cast", SpellCastInput.class);

        assertEquals(3, binder.schema().fields().size());
        assertEquals(Target.class, binder.schema().objects().get("target"));

        Target target = new Target();
        PiGraphContext context = binder.bind(new SpellCastInput(new Object(), target, 12.5D));

        assertEquals(12.5D, context.number("baseDamage"));
        assertEquals(target, context.object("target", Target.class).orElseThrow());
    }

    @Test
    void engineRunnerAcceptsGraphContextBinder() {
        PiGraphContextBinder<SpellCastInput> binder = PiGraphContextBindings.record("spell_cast", SpellCastInput.class);
        PiEngineRunner<SpellCastInput> runner = PiEngineRunner.actionRegistry(binder);

        PiEngineFrame frame = runner.run(new ReadBaseDamageAction(), new SpellCastInput(new Object(), new Target(), 12.5D));

        assertEquals(12.5D, frame.number("observed"), 0.0001D);
    }

    private record SpellCastInput(Object caster, Target target, double baseDamage) {
    }

    private static final class Target {
    }

    private record ReadBaseDamageAction() implements PiEngineAction {
        private static final PiEngineActionType<ReadBaseDamageAction> TYPE = PiEngineActionType.of(
                id("test:read_base_damage"),
                ignored -> com.mojang.serialization.Codec.unit(new ReadBaseDamageAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(org.pickaid.pidatagraph.engine.PiEngineContext context) {
            return PiEngineFrame.builder().number("observed", context.number("baseDamage")).build();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder().number("baseDamage").build();
        }
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
