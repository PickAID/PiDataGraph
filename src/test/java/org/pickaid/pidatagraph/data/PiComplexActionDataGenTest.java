package org.pickaid.pidatagraph.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

class PiComplexActionDataGenTest {
    @Test
    void generatorCanWriteNestedPolymorphicActionJson() {
        PiDataSet<ActionSpec> actions = PiDataCatalog.builder(actionDefinition(), "examplemod")
                .add(new CombatActionEntries())
                .buildSet();

        List<PiDataJsonFile> files = actions.encode();

        assertEquals("data/examplemod/action/flame_combo.json", files.get(0).path());
        assertEquals(JsonParser.parseString("""
                {
                  "type": "root",
                  "child": {
                    "type": "selector",
                    "target": "enemy",
                    "range": "6 + spellPower",
                    "child": {
                      "type": "repeat",
                      "times": "min(3 + level, 8)",
                      "child": {
                        "type": "move_target",
                        "forward": "1.25",
                        "up": "0.2",
                        "child": {
                          "type": "damage",
                          "amount": "base + spellPower * 2"
                        }
                      }
                    }
                  }
                }
                """), files.get(0).json());
    }

    @Test
    void nestedActionVerificationReportsTheFullPath() {
        PiDataSet<ActionSpec> actions = PiDataCatalog.builder(actionDefinition(), "examplemod")
                .add(builder -> builder.entry("broken_combo", root(
                        selector("enemy", "6 + missing", repeat("2", damage("base"))))))
                .buildSet();

        PiDataValidation validation = actions.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base", "spellPower", "level"))
                .build());

        assertEquals(1, validation.issues().size());
        assertEquals("examplemod:broken_combo/root.child.range", validation.issues().get(0).path());
        assertEquals("invalid expression `6 + missing` for variables [base, spellPower, level]",
                validation.issues().get(0).message());
    }

    private static PiDataDefinition<ActionSpec> actionDefinition() {
        return PiDataDefinition.<ActionSpec>builder(id("action"), "action", ACTION_CODEC)
                .verify("root", (context, action) -> action.verify(context, "root"))
                .build();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }

    private static RootAction root(ActionSpec child) {
        return new RootAction(child);
    }

    private static SelectorAction selector(String target, String range, ActionSpec child) {
        return new SelectorAction(target, PiDoubleExpression.of(range), child);
    }

    private static RepeatAction repeat(String times, ActionSpec child) {
        return new RepeatAction(PiIntExpression.of(times), child);
    }

    private static MoveTargetAction moveTarget(String forward, String up, ActionSpec child) {
        return new MoveTargetAction(PiDoubleExpression.of(forward), PiDoubleExpression.of(up), child);
    }

    private static DamageAction damage(String amount) {
        return new DamageAction(PiDoubleExpression.of(amount));
    }

    private static final class CombatActionEntries implements PiDataGenEntry<ActionSpec> {
        @Override
        public void register(PiDataSet.Builder<ActionSpec> builder) {
            builder.entry("flame_combo", root(
                    selector("enemy", "6 + spellPower",
                            repeat("min(3 + level, 8)",
                                    moveTarget("1.25", "0.2",
                                            damage("base + spellPower * 2"))))));
        }
    }

    private interface ActionSpec {
        Codec<ActionSpec> ACTION_CODEC = ExtraCodecs.lazyInitializedCodec(() ->
                ActionType.CODEC.dispatch("type", ActionSpec::type, ActionType::codec));

        ActionType<?> type();

        void verify(PiDataBuildContext context, String path);
    }

    private static final Codec<ActionSpec> ACTION_CODEC = ActionSpec.ACTION_CODEC;

    private record ActionType<T extends ActionSpec>(String id, Codec<T> codec) {
        private static final ActionType<RootAction> ROOT = new ActionType<>("root", RootAction.CODEC);
        private static final ActionType<SelectorAction> SELECTOR = new ActionType<>("selector", SelectorAction.CODEC);
        private static final ActionType<RepeatAction> REPEAT = new ActionType<>("repeat", RepeatAction.CODEC);
        private static final ActionType<MoveTargetAction> MOVE_TARGET = new ActionType<>("move_target", MoveTargetAction.CODEC);
        private static final ActionType<DamageAction> DAMAGE = new ActionType<>("damage", DamageAction.CODEC);
        private static final Map<String, ActionType<?>> BY_ID = Map.of(
                ROOT.id, ROOT,
                SELECTOR.id, SELECTOR,
                REPEAT.id, REPEAT,
                MOVE_TARGET.id, MOVE_TARGET,
                DAMAGE.id, DAMAGE);
        private static final Codec<ActionType<?>> CODEC = Codec.STRING.xmap(BY_ID::get, ActionType::id);
    }

    private record RootAction(ActionSpec child) implements ActionSpec {
        private static final Codec<RootAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ACTION_CODEC.fieldOf("child").forGetter(RootAction::child)
        ).apply(instance, RootAction::new));

        @Override
        public ActionType<?> type() {
            return ActionType.ROOT;
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            child.verify(context, path + ".child");
        }
    }

    private record SelectorAction(String target, PiDoubleExpression range, ActionSpec child) implements ActionSpec {
        private static final Codec<SelectorAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("target").forGetter(SelectorAction::target),
                PiDoubleExpression.CODEC.fieldOf("range").forGetter(SelectorAction::range),
                ACTION_CODEC.fieldOf("child").forGetter(SelectorAction::child)
        ).apply(instance, SelectorAction::new));

        @Override
        public ActionType<?> type() {
            return ActionType.SELECTOR;
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            check(path + ".range", () -> range.compile(context.expressionLanguage(), context.expressionScope()));
            child.verify(context, path + ".child");
        }
    }

    private record RepeatAction(PiIntExpression times, ActionSpec child) implements ActionSpec {
        private static final Codec<RepeatAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PiIntExpression.CODEC.fieldOf("times").forGetter(RepeatAction::times),
                ACTION_CODEC.fieldOf("child").forGetter(RepeatAction::child)
        ).apply(instance, RepeatAction::new));

        @Override
        public ActionType<?> type() {
            return ActionType.REPEAT;
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            check(path + ".times", () -> times.compile(context.expressionLanguage(), context.expressionScope()));
            child.verify(context, path + ".child");
        }
    }

    private record MoveTargetAction(PiDoubleExpression forward, PiDoubleExpression up, ActionSpec child) implements ActionSpec {
        private static final Codec<MoveTargetAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PiDoubleExpression.CODEC.fieldOf("forward").forGetter(MoveTargetAction::forward),
                PiDoubleExpression.CODEC.fieldOf("up").forGetter(MoveTargetAction::up),
                ACTION_CODEC.fieldOf("child").forGetter(MoveTargetAction::child)
        ).apply(instance, MoveTargetAction::new));

        @Override
        public ActionType<?> type() {
            return ActionType.MOVE_TARGET;
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            check(path + ".forward", () -> forward.compile(context.expressionLanguage(), context.expressionScope()));
            check(path + ".up", () -> up.compile(context.expressionLanguage(), context.expressionScope()));
            child.verify(context, path + ".child");
        }
    }

    private record DamageAction(PiDoubleExpression amount) implements ActionSpec {
        private static final Codec<DamageAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PiDoubleExpression.CODEC.fieldOf("amount").forGetter(DamageAction::amount)
        ).apply(instance, DamageAction::new));

        @Override
        public ActionType<?> type() {
            return ActionType.DAMAGE;
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            check(path + ".amount", () -> amount.compile(context.expressionLanguage(), context.expressionScope()));
        }
    }

    private static void check(String path, Runnable verifier) {
        try {
            verifier.run();
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(path, error);
        }
    }
}
