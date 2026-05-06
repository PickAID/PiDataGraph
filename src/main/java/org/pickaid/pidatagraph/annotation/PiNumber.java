package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the generated context number key for a record component.
 *
 * <p>The annotated component must be a primitive numeric type or a
 * {@link java.lang.Number} subtype. Generated binders reject null
 * {@code Number} values before they enter the engine context.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.RECORD_COMPONENT)
public @interface PiNumber {
    /**
     * Context variable name used by expressions and action contracts.
     */
    String value();
}
