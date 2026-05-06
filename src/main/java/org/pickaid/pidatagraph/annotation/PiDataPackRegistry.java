package org.pickaid.pidatagraph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.pickaid.pidatagraph.data.PiDataPackSync;

/**
 * Marks a static {@code PiEngineActionRegistry} field for generated datapack registry glue.
 *
 * <p>The field must be declared inside a {@link PiDataGraphModule} class. The
 * processor generates the registry key, data definition, automatic Forge event
 * subscribers, manual registration helpers, and reload-verifier wiring from
 * this declaration.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface PiDataPackRegistry {
    /**
     * Registry key path, for example {@code hit_action}.
     */
    String path();

    /**
     * Optional data folder used by generated data-definition helpers.
     *
     * <p>When blank, the registry {@link #path()} is used as the folder.
     */
    String folder() default "";

    /**
     * Whether this datapack registry should be server-only or synced to clients.
     */
    PiDataPackSync sync() default PiDataPackSync.SERVER_ONLY;
}
