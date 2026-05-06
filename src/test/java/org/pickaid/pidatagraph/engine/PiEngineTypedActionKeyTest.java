package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEmitNumberAction;
import org.pickaid.pidatagraph.engine.action.PiEmitObjectAction;
import org.pickaid.pidatagraph.engine.action.PiForEachObjectAction;
import org.pickaid.pidatagraph.engine.action.PiSequenceAction;
import org.pickaid.pidatagraph.engine.action.PiWithContextAction;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

class PiEngineTypedActionKeyTest {
    @Test
    void withContextBuilderAcceptsTypedJavaKeys() {
        PiEngineNumberKey base = PiEngineNumberKey.of("base");
        PiEngineNumberKey power = PiEngineNumberKey.of("power");
        PiEngineNumberKey scaled = PiEngineNumberKey.of("scaled");
        PiEngineContextKey<String> hitEntity = PiEngineContextKey.of("hitEntity", String.class);
        PiEngineContextKey<String> target = PiEngineContextKey.of("target", String.class);
        PiEngineValueKey<Number> finalDamage = PiEngineValueKey.number("damage.final");
        PiEngineValueKey<String> selectedTarget = PiEngineValueKey.object("target.selected", String.class);

        PiWithContextAction action = PiWithContextAction.builder(new PiSequenceAction(List.of(
                        new PiEmitNumberAction(finalDamage, PiDoubleExpression.of("scaled")),
                        new PiEmitObjectAction(selectedTarget, target))))
                .number(scaled, PiDoubleExpression.of("base + power * 2"))
                .object(target, hitEntity)
                .build();

        assertEquals(String.class, action.contextContract().objectType(hitEntity).orElseThrow());

        PiEngineFrame frame = action.execute(PiEngineContext.builder()
                .number(base, 3)
                .number(power, 2)
                .object(hitEntity, "zombie")
                .build());

        assertEquals(7.0, frame.number(finalDamage), 0.0001);
        assertEquals("zombie", frame.value(selectedTarget));
    }

    @Test
    void forEachObjectVerificationUsesTheTypedItemKeyType() {
        PiEngineContextKey<List> targets = PiEngineContextKey.of("targets", List.class);
        PiEngineContextKey<String> stringTarget = PiEngineContextKey.of("target", String.class);
        PiEngineContextKey<Integer> integerTarget = PiEngineContextKey.of("target", Integer.class);
        PiEngineValueKey<Integer> selected = PiEngineValueKey.object("selected", Integer.class);

        PiForEachObjectAction action = new PiForEachObjectAction(
                targets,
                stringTarget,
                new PiEmitObjectAction(selected, integerTarget));

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                action.verify(PiDataBuildContext.builder()
                        .object(targets)
                        .build(), "root"));

        assertEquals("root.child.context.objects.target", error.path());
        assertEquals("engine context object `target` must be java.lang.Integer, but validation context provides java.lang.String",
                error.getMessage());
    }

    @Test
    void forEachObjectRuntimeChecksTheTypedItemKeyType() {
        PiEngineContextKey<List> targets = PiEngineContextKey.of("targets", List.class);
        PiEngineContextKey<String> target = PiEngineContextKey.of("target", String.class);
        PiForEachObjectAction action = new PiForEachObjectAction(
                targets,
                target,
                new PiSequenceAction(List.of()));

        ClassCastException error = assertThrows(ClassCastException.class, () ->
                action.execute(PiEngineContext.builder()
                        .object(targets, List.of(42))
                        .build()));

        assertEquals("engine context object list `targets` item `target` at index 0 is java.lang.Integer, not java.lang.String",
                error.getMessage());
    }
}
