package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a record as a compile-time input contract for one generated {@code PiEngineRunner}.
 *
 * <p>Record components become context entries. Primitive numeric components
 * and {@link java.lang.Number} components become numbers by default.
 * Non-primitive non-number components become objects by default.
 * Use {@link PiNumber} or {@link PiObject} when the context key should differ
 * from the Java component name.
 *
 * <p>One generated datapack registry may have only one input contract. If two
 * gameplay flows need different input records, declare separate
 * {@link PiDataPackRegistry} fields so reload validation uses the right
 * contract for each registry.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface PiGraphInput {
    /**
     * Registry field reference. Use the simple field name when it is unique,
     * or {@code ModuleName.FIELD_NAME} when multiple modules share a field name.
     */
    String registry();

    /**
     * Java identifier used as the generated runner and binder prefix.
     */
    String name();

    /**
     * Optional public facade class generated for normal project code.
     *
     * <p>When set, the processor generates this class in the datapack registry
     * module package. The facade delegates to the internal {@code _PiDataGraph}
     * glue and exposes stable {@code Context}, {@code Output}, {@code key},
     * {@code dataSet}, {@code validationContext}, {@code run}, and registration
     * helpers.
     */
    String facade() default "";
}
