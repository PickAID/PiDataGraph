package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PiDataGraphProcessorStructureTest {
    @Test
    void processorClassesStaySmallEnoughToReview() throws IOException {
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessor.java", 280);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphGlueGenerator.java", 260);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphComponentSupport.java", 140);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessorTypes.java", 120);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessorNames.java", 100);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphModuleSupport.java", 80);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphGeneratedNames.java", 100);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphOutputSupport.java", 180);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphOutputGlueWriter.java", 120);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphRegistryGlueWriter.java", 80);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphGeneratedMemberSet.java", 80);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphFacadeSupport.java", 80);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphFacadeGenerator.java", 180);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphFacadeKeyWriter.java", 90);
        assertLineCount("src/main/java/org/pickaid/pidatagraph/processor/PiDataGraphEventSubscriberGenerator.java", 100);
        assertLineCount("src/test/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessorTest.java", 600);
        assertLineCount("src/test/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessorValidationTest.java", 520);
        assertLineCount("src/test/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessorGuardrailTest.java", 160);
        assertLineCount("src/test/java/org/pickaid/pidatagraph/processor/PiDataGraphProcessorVisibilityGuardrailTest.java", 140);
        assertLineCount("src/test/java/org/pickaid/pidatagraph/processor/PiDataGraphGeneratedFacadeTest.java", 180);
        assertLineCount("src/test/java/org/pickaid/pidatagraph/processor/PiDataGraphGeneratedNamedFacadeTest.java", 160);
    }

    private static void assertLineCount(String path, int maxLines) throws IOException {
        long lines = Files.lines(Path.of(path)).count();
        assertTrue(lines <= maxLines, path + " has " + lines + " lines; keep it at or below " + maxLines);
    }
}
