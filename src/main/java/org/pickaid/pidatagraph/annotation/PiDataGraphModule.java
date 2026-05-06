package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks one class as the compile-time source-generation root for PiDataGraph registries.
 *
 * <p>The annotated class should own the static registry fields annotated with
 * {@link PiDataPackRegistry}. PiDataGraph does not scan this annotation at
 * runtime; the annotation processor uses it to generate explicit helper code.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface PiDataGraphModule {
    /**
     * ResourceLocation namespace used by generated registry keys.
     */
    String modid();
}
