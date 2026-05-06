package org.pickaid.pidatagraph.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import org.pickaid.pidatagraph.annotation.PiGraphInput;
import org.pickaid.pidatagraph.annotation.PiNumber;
import org.pickaid.pidatagraph.annotation.PiObject;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Component;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphComponentSupport {
    private final ProcessingEnvironment environment;
    private final PiDataGraphProcessorTypes types;
    private final BiConsumer<Element, String> error;

    PiDataGraphComponentSupport(
            ProcessingEnvironment environment,
            PiDataGraphProcessorTypes types,
            BiConsumer<Element, String> error
    ) {
        this.environment = environment;
        this.types = types;
        this.error = error;
    }

    List<Component> validate(TypeElement input, Registry registry) {
        Map<String, RecordComponentElement> keys = new LinkedHashMap<>();
        Map<String, Element> generatedKeyNames = new LinkedHashMap<>();
        List<Component> components = new ArrayList<>();
        String inputPrefix = PiDataGraphGeneratedNames.inputPrefix(input.getAnnotation(PiGraphInput.class).name().trim());
        generatedKeyNames.put(inputPrefix, input);
        generatedKeyNames.put(inputPrefix + "_BINDER", input);
        for (RecordComponentElement component : input.getRecordComponents()) {
            PiNumber number = component.getAnnotation(PiNumber.class);
            PiObject object = component.getAnnotation(PiObject.class);
            if (number != null && object != null) {
                error.accept(component, "record component `" + component.getSimpleName() + "` cannot use both @PiNumber and @PiObject");
                continue;
            }
            String key = contextKey(component, number, object);
            validateContextKey(component, key);
            Element previous = keys.putIfAbsent(key, component);
            if (previous != null) {
                error.accept(component, "duplicate PiDataGraph context key `" + key + "`");
            }
            String generatedName = inputPrefix + "_" + PiDataGraphGeneratedNames.inputPrefix(key);
            previous = generatedKeyNames.putIfAbsent(generatedName, component);
            if (previous != null) {
                error.accept(component, "PiGraphInput component key `" + key
                        + "` generates duplicate member `" + generatedName + "`");
            }
            validateComponentClassification(component, number, object);
            validateObjectTypeVisibility(component, number, registry);
            if (types.isNumeric(component.asType()) && object == null) {
                components.add(new Component(key, component.getSimpleName().toString(), "", true, !component.asType().getKind().isPrimitive()));
            } else if (!component.asType().getKind().isPrimitive()) {
                components.add(new Component(key, component.getSimpleName().toString(), types.objectType(component), false, false));
            }
        }
        return components;
    }

    private String contextKey(RecordComponentElement component, PiNumber number, PiObject object) {
        if (number != null) {
            return number.value().trim();
        }
        if (object != null) {
            return object.value().trim();
        }
        return component.getSimpleName().toString();
    }

    private void validateContextKey(RecordComponentElement component, String key) {
        try {
            PiEngineKeyNames.variable(key);
        } catch (IllegalArgumentException error) {
            this.error.accept(component, "invalid PiDataGraph context key `" + key + "`");
        }
    }

    private void validateObjectTypeVisibility(RecordComponentElement component, PiNumber number, Registry registry) {
        if (registry == null || number != null || component.asType().getKind().isPrimitive()
                || types.isNumeric(component.asType())) {
            return;
        }
        Element runtimeElement = types.runtimeTypeElement(component.asType());
        if (runtimeElement != null && types.hasPrivateEnclosingType(runtimeElement)) {
            error.accept(component, "PiGraphInput object component `" + component.getSimpleName()
                    + "` type enclosing types must not be private");
            return;
        }
        if (runtimeElement != null && !typePackage(runtimeElement).equals(registry.modulePackage())
                && !types.isPubliclyAccessible(runtimeElement)) {
            error.accept(component, "PiGraphInput object component `" + component.getSimpleName()
                    + "` type and enclosing types must be public when it is used from another package");
            return;
        }
        if (runtimeElement == null || runtimeElement.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)) {
            return;
        }
        if (!typePackage(runtimeElement).equals(registry.modulePackage())) {
            error.accept(component, "PiGraphInput object component `" + component.getSimpleName()
                    + "` type must be public when it is used from another package");
        }
    }

    private String typePackage(Element element) {
        return environment.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    private void validateComponentClassification(RecordComponentElement component, PiNumber number, PiObject object) {
        if (number != null) {
            if (!types.isNumeric(component.asType())) {
                error.accept(component, "@PiNumber component `" + component.getSimpleName() + "` must be numeric");
            }
            return;
        }
        if (object != null) {
            if (component.asType().getKind().isPrimitive()) {
                error.accept(component, "@PiObject component `" + component.getSimpleName() + "` must not be primitive");
            } else if (types.hasPrivateRuntimeType(component.asType())) {
                error.accept(component, "PiGraphInput object component `" + component.getSimpleName() + "` type must not be private");
            }
            return;
        }
        if (!types.isNumeric(component.asType()) && component.asType().getKind().isPrimitive()) {
            error.accept(component, "primitive PiGraphInput component `" + component.getSimpleName() + "` must be numeric or explicitly mapped");
        } else if (!types.isNumeric(component.asType()) && types.hasPrivateRuntimeType(component.asType())) {
            error.accept(component, "PiGraphInput object component `" + component.getSimpleName() + "` type must not be private");
        }
    }
}
