package org.pickaid.pidatagraph.processor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphFacadeSupport {
    private final BiConsumer<Element, String> error;
    private final Map<String, Element> names = new LinkedHashMap<>();

    PiDataGraphFacadeSupport(BiConsumer<Element, String> error) {
        this.error = error;
    }

    void validate(Element input, Registry registry, String facadeName) {
        if (facadeName.isEmpty()) {
            return;
        }
        if (!SourceVersion.isIdentifier(facadeName) || SourceVersion.isKeyword(facadeName)) {
            error.accept(input, "PiGraphInput facade must be a Java identifier");
            return;
        }
        String generatedModuleName = PiDataGraphGeneratedNames.moduleName(
                registry.moduleQualifiedName(),
                registry.modulePackage());
        if (facadeName.equals(generatedModuleName)) {
            error.accept(input, "PiGraphInput facade `" + facadeName + "` conflicts with generated module glue");
        }
        String qualified = registry.modulePackage().isEmpty() ? facadeName : registry.modulePackage() + "." + facadeName;
        if (names.putIfAbsent(qualified, input) != null) {
            error.accept(input, "duplicate PiGraphInput facade `" + qualified + "`");
        }
    }
}
