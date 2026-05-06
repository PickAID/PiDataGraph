package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the generated frame value key for a {@link PiGraphOutput} component.
 *
 * <p>Use this when the Java record component name should stay idiomatic while
 * the emitted frame key uses a dotted path, for example {@code damage.final}
 * or {@code hud.mana_fill}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.RECORD_COMPONENT)
public @interface PiOutput {
    /**
     * Frame value key used by generated {@code PiEngineValueKey} constants.
     */
    String value();
}
