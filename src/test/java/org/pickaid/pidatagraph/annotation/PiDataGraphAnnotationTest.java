package org.pickaid.pidatagraph.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataPackSync;

class PiDataGraphAnnotationTest {
    @Test
    void annotationsAreSourceGenerationContracts() throws ReflectiveOperationException {
        assertSourceRetention(PiDataGraphModule.class);
        assertSourceRetention(PiDataPackRegistry.class);
        assertSourceRetention(PiGraphInput.class);
        assertSourceRetention(PiGraphOutput.class);
        assertSourceRetention(PiNumber.class);
        assertSourceRetention(PiObject.class);
        assertSourceRetention(PiOutput.class);

        assertArrayEquals(new ElementType[]{ElementType.TYPE}, target(PiDataGraphModule.class));
        assertArrayEquals(new ElementType[]{ElementType.FIELD}, target(PiDataPackRegistry.class));
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, target(PiGraphInput.class));
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, target(PiGraphOutput.class));
        assertArrayEquals(new ElementType[]{ElementType.RECORD_COMPONENT}, target(PiNumber.class));
        assertArrayEquals(new ElementType[]{ElementType.RECORD_COMPONENT}, target(PiObject.class));
        assertArrayEquals(new ElementType[]{ElementType.RECORD_COMPONENT}, target(PiOutput.class));

        assertEquals(PiDataPackSync.SERVER_ONLY, PiDataPackRegistry.class.getMethod("sync").getDefaultValue());
        assertEquals("", PiDataPackRegistry.class.getMethod("folder").getDefaultValue());
    }

    private static void assertSourceRetention(Class<?> annotation) {
        assertEquals(RetentionPolicy.SOURCE, annotation.getAnnotation(Retention.class).value());
    }

    private static ElementType[] target(Class<?> annotation) {
        return annotation.getAnnotation(Target.class).value();
    }
}
