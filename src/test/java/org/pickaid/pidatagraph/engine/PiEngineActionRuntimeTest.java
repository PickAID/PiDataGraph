package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.action.PiIfAction;
import org.pickaid.pidatagraph.engine.action.PiRepeatAction;
import org.pickaid.pidatagraph.engine.action.PiSequenceAction;
import org.pickaid.pidatagraph.engine.action.PiWithNumberAction;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

class PiEngineActionRuntimeTest {
    @Test
    void nestedActionDataCanBeEncodedAndExecutedWithLayeredContext() {
        PiEngineActionRegistry registry = PiEngineActionRegistry.builder()
                .install(PiEngineActions.core())
                .add(DamageProbeAction.TYPE)
                .build();
        PiEngineAction action = new PiSequenceAction(List.of(
                new PiIfAction(
                        PiBooleanExpression.of("mana >= cost"),
                        new PiRepeatAction(
                                PiIntExpression.of("min(2 + level, 4)"),
                                "hit",
                                new PiWithNumberAction(
                                        "damage",
                                        PiDoubleExpression.of("base + spellPower * (hit + 1)"),
                                        new DamageProbeAction(PiDoubleExpression.of("damage"))
                                )
                        ),
                        null
                )
        ));

        JsonElement encoded = registry.codec()
                .encodeStart(JsonOps.INSTANCE, action)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals(JsonParser.parseString("""
                {
                  "type": "pidatagraph:sequence",
                  "children": [
                    {
                      "type": "pidatagraph:if",
                      "condition": "mana >= cost",
                      "then": {
                        "type": "pidatagraph:repeat",
                        "times": "min(2 + level, 4)",
                        "index": "hit",
                        "child": {
                          "type": "pidatagraph:with_number",
                          "name": "damage",
                          "value": "base + spellPower * (hit + 1)",
                          "child": {
                            "type": "example:damage_probe",
                            "amount": "damage"
                          }
                        }
                      }
                    }
                  ]
                }
                """), encoded);

        CastLog log = new CastLog();
        action.execute(PiEngineContext.builder()
                .number("mana", 30)
                .number("cost", 8)
                .number("level", 1)
                .number("base", 3)
                .number("spellPower", 4)
                .object("castLog", log)
                .build());

        assertEquals(List.of(
                new DamageEvent(0, 7.0),
                new DamageEvent(1, 11.0),
                new DamageEvent(2, 15.0)
        ), log.events());
    }

    @Test
    void nestedActionVerificationCanSeeVariablesIntroducedByParentNodes() {
        PiEngineAction action = new PiRepeatAction(
                PiIntExpression.of("2"),
                "hit",
                new PiWithNumberAction(
                        "damage",
                        PiDoubleExpression.of("base + hit"),
                        new DamageProbeAction(PiDoubleExpression.of("damage"))
                )
        );

        action.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base"))
                .build(), "spell.root");
    }

    @Test
    void nestedActionVerificationReportsFullPathsForBrokenExpressions() {
        PiEngineAction action = new PiRepeatAction(
                PiIntExpression.of("missing + 1"),
                "hit",
                new DamageProbeAction(PiDoubleExpression.of("base"))
        );

        try {
            action.verify(PiDataBuildContext.builder()
                    .expressionScope(PiExpressionScope.of("base"))
                    .build(), "spell.root");
        } catch (PiDataVerificationException error) {
            assertEquals("spell.root.times", error.path());
            return;
        }
        throw new AssertionError("expected verification failure");
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("example", path);
    }

    private record DamageProbeAction(PiDoubleExpression amount) implements PiEngineAction {
        private static final PiEngineActionType<DamageProbeAction> TYPE = PiEngineActionType.of(
                id("damage_probe"),
                actionCodec -> RecordCodecBuilder.create(instance -> instance.group(
                        PiDoubleExpression.CODEC.fieldOf("amount").forGetter(DamageProbeAction::amount)
                ).apply(instance, DamageProbeAction::new))
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            context.object("castLog", CastLog.class)
                    .orElseThrow()
                    .add((int) context.number("hit"), context.evaluate(amount));
            return PiEngineFrame.empty();
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            PiEngineActions.verify(path + ".amount", context, amount);
        }
    }

    private record DamageEvent(int hit, double amount) {
    }

    private static final class CastLog {
        private final List<DamageEvent> events = new ArrayList<>();

        private void add(int hit, double amount) {
            events.add(new DamageEvent(hit, amount));
        }

        private List<DamageEvent> events() {
            return List.copyOf(events);
        }
    }
}
