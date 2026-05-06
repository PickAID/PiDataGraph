package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a record as a compile-time frame-output contract for one generated runner.
 *
 * <p>Record components become {@code PiEngineValueKey} constants in the
 * generated glue class. Numeric components become number keys, boolean
 * components become flag keys, and reference components become typed object
 * value keys.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface PiGraphOutput {
    /**
     * Registry field reference. Use the same value as the matching {@link PiGraphInput}.
     */
    String registry();

    /**
     * Java identifier used as the generated output key prefix.
     */
    String name();
}
