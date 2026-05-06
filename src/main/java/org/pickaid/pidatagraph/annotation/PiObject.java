package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the generated context object key for a record component.
 *
 * <p>The annotated component must be a reference type. Primitive numbers should
 * use {@link PiNumber} or the default numeric mapping.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.RECORD_COMPONENT)
public @interface PiObject {
    /**
     * Context object name used by action and predicate contracts.
     */
    String value();
}
