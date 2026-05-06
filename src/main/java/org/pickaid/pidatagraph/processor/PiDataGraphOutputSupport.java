package org.pickaid.pidatagraph.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import org.pickaid.pidatagraph.annotation.PiOutput;
import org.pickaid.pidatagraph.annotation.PiGraphOutput;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Input;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Output;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.OutputComponent;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.OutputKind;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphOutputSupport {
    private final ProcessingEnvironment environment;
    private final PiDataGraphProcessorTypes types;
    private final BiConsumer<Element, String> error;

    PiDataGraphOutputSupport(ProcessingEnvironment environment, PiDataGraphProcessorTypes types, BiConsumer<Element, String> error) {
        this.environment = environment;
        this.types = types;
        this.error = error;
    }

    List<Output> validate(RoundEnvironment roundEnv, List<Registry> registries, List<Input> inputs) {
        List<Output> outputs = new ArrayList<>();
        Map<String, Element> names = new LinkedHashMap<>();
        Map<String, Element> prefixes = new LinkedHashMap<>();
        Map<String, Set<String>> reservedMembers = PiDataGraphGeneratedMemberSet.inputMembers(inputs);
        for (Element output : roundEnv.getElementsAnnotatedWith(PiGraphOutput.class)) {
            PiGraphOutput annotation = output.getAnnotation(PiGraphOutput.class);
            String name = annotation.name().trim();
            validateName(output, name);
            Registry registry = resolveRegistry(output, annotation.registry().trim(), registries);
            if (registry != null && !name.isEmpty()) {
                String scoped = registry.moduleQualifiedName() + "#" + name;
                duplicate(names, scoped, output, "duplicate PiGraphOutput name `" + name + "`");
                scoped = registry.moduleQualifiedName() + "#" + PiDataGraphGeneratedNames.inputPrefix(name);
                duplicate(prefixes, scoped, output, "PiGraphOutput name `" + name + "` generates duplicate member prefix");
            }
            if (!validateRecordShape(output, registry)) {
                continue;
            }
            String prefix = PiDataGraphGeneratedNames.inputPrefix(name);
            Set<String> reserved = registry == null ? Set.of() : reservedMembers.getOrDefault(PiDataGraphGeneratedMemberSet.registryKey(registry), Set.of());
            List<OutputComponent> components = validateComponents((TypeElement) output, registry, prefix, reserved);
            if (registry != null) {
                outputs.add(new Output((TypeElement) output, registry, name, components));
            }
        }
        return outputs;
    }

    private void validateName(Element output, String name) {
        if (name.isEmpty()) {
            error.accept(output, "PiGraphOutput name must not be blank");
        } else if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name)) {
            error.accept(output, "PiGraphOutput name must be a Java identifier");
        }
    }

    private boolean validateRecordShape(Element output, Registry registry) {
        if (output.getKind() != ElementKind.RECORD) {
            error.accept(output, "@PiGraphOutput can only be applied to records");
            return false;
        }
        if (output.getModifiers().contains(Modifier.PRIVATE)) {
            error.accept(output, "@PiGraphOutput record must not be private");
            return false;
        }
        if (types.hasPrivateEnclosingType(output)) {
            error.accept(output, "@PiGraphOutput record enclosing types must not be private");
            return false;
        }
        if (registry != null && types.isDifferentPackage(output, registry)
                && !types.isPubliclyAccessible(output)) {
            error.accept(output, "@PiGraphOutput record and enclosing types must be public when used from another package");
            return false;
        }
        return true;
    }

    private List<OutputComponent> validateComponents(TypeElement output, Registry registry, String prefix, Set<String> reserved) {
        List<OutputComponent> components = new ArrayList<>();
        Map<String, Element> keys = new LinkedHashMap<>();
        Map<String, Element> generatedNames = new LinkedHashMap<>();
        for (RecordComponentElement component : output.getRecordComponents()) {
            String key = outputKey(component);
            validateFrameKey(component, key);
            duplicate(keys, key, component, "duplicate PiDataGraph output key `" + key + "`");
            String generatedName = PiDataGraphGeneratedMemberSet.component(prefix, key);
            if (reserved.contains(generatedName)) {
                error.accept(component, "PiGraphOutput component key `" + key
                        + "` generates duplicate member `" + generatedName + "`");
            }
            duplicate(generatedNames, generatedName, component, "PiGraphOutput component key `" + key
                    + "` generates duplicate member `" + generatedName + "`");
            if (types.isNumeric(component.asType())) {
                components.add(new OutputComponent(key, component.asType().toString(), OutputKind.NUMBER));
            } else if (types.isBoolean(component.asType())) {
                components.add(new OutputComponent(key, component.asType().toString(), OutputKind.FLAG));
            } else if (component.asType().getKind().isPrimitive()) {
                error.accept(component, "primitive PiGraphOutput component `" + key + "` must be numeric or boolean");
            } else {
                validateObjectType(component, registry);
                components.add(new OutputComponent(key, types.objectType(component), OutputKind.OBJECT));
            }
        }
        return components;
    }

    private String outputKey(RecordComponentElement component) {
        PiOutput output = component.getAnnotation(PiOutput.class);
        return output == null ? component.getSimpleName().toString() : output.value().trim();
    }

    private void validateFrameKey(RecordComponentElement component, String key) {
        try {
            PiEngineKeyNames.frameValue(key);
        } catch (IllegalArgumentException error) {
            this.error.accept(component, "invalid PiDataGraph output key `" + key + "`");
        }
    }

    private void validateObjectType(RecordComponentElement component, Registry registry) {
        Element runtime = types.runtimeTypeElement(component.asType());
        if (types.hasPrivateRuntimeType(component.asType())
                || (runtime != null && types.hasPrivateEnclosingType(runtime))) {
            error.accept(component, "PiGraphOutput object component `" + component.getSimpleName() + "` type must not be private");
        } else if (registry != null && runtime != null && !typePackage(runtime).equals(registry.modulePackage())
                && !types.isPubliclyAccessible(runtime)) {
            error.accept(component, "PiGraphOutput object component `" + component.getSimpleName()
                    + "` type and enclosing types must be public when it is used from another package");
        }
    }

    private Registry resolveRegistry(Element output, String reference, List<Registry> registries) {
        if (reference.isEmpty()) {
            error.accept(output, "PiGraphOutput registry must not be blank");
            return null;
        }
        List<Registry> matches = registries.stream()
                .filter(registry -> reference.equals(registry.fieldName())
                        || reference.equals(registry.moduleSimpleName() + "." + registry.fieldName())
                        || reference.equals(registry.moduleQualifiedName() + "." + registry.fieldName()))
                .toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        error.accept(output, matches.isEmpty() ? "unknown PiGraphOutput registry `" + reference + "`"
                : "ambiguous PiGraphOutput registry `" + reference + "`; use a fully qualified module name");
        return null;
    }

    private void duplicate(Map<String, Element> map, String key, Element element, String message) {
        if (map.putIfAbsent(key, element) != null) {
            error.accept(element, message);
        }
    }

    private String typePackage(Element element) {
        return environment.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }
}
