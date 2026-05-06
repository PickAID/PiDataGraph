package org.pickaid.pidatagraph.processor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.pickaid.pidatagraph.annotation.PiDataGraphModule;

final class PiDataGraphModuleSupport {
    private final ProcessingEnvironment environment;
    private final BiConsumer<Element, String> error;

    PiDataGraphModuleSupport(ProcessingEnvironment environment, BiConsumer<Element, String> error) {
        this.environment = environment;
        this.error = error;
    }

    void validateGeneratedModuleNames(RoundEnvironment roundEnv) {
        Map<String, Element> names = new LinkedHashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(PiDataGraphModule.class)) {
            if (element instanceof TypeElement module) {
                validateGeneratedModuleName(names, module);
            }
        }
    }

    private void validateGeneratedModuleName(Map<String, Element> names, TypeElement module) {
        String packageName = environment.getElementUtils().getPackageOf(module).getQualifiedName().toString();
        String generatedName = PiDataGraphGeneratedNames.moduleName(module, packageName);
        String qualifiedName = packageName.isEmpty() ? generatedName : packageName + "." + generatedName;
        Element previous = names.putIfAbsent(qualifiedName, module);
        if (previous != null) {
            error.accept(module, "duplicate PiDataGraph generated helper class `" + qualifiedName + "`");
        }
    }
}
