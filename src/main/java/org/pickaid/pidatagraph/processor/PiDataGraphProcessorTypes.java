package org.pickaid.pidatagraph.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphProcessorTypes {
    private static final String NUMBER = "java.lang.Number";

    private final ProcessingEnvironment environment;

    PiDataGraphProcessorTypes(ProcessingEnvironment environment) {
        this.environment = environment;
    }

    TypeMirror type(String className) {
        TypeElement element = environment.getElementUtils().getTypeElement(className);
        if (element == null) {
            throw new IllegalStateException("missing processor classpath type " + className);
        }
        return element.asType();
    }

    boolean isNumeric(TypeMirror type) {
        TypeKind kind = type.getKind();
        if (kind == TypeKind.BYTE || kind == TypeKind.SHORT || kind == TypeKind.INT
                || kind == TypeKind.LONG || kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            return true;
        }
        if (kind != TypeKind.DECLARED) {
            return false;
        }
        return environment.getTypeUtils().isAssignable(
                environment.getTypeUtils().erasure(type),
                environment.getTypeUtils().erasure(type(NUMBER)));
    }

    boolean isBoolean(TypeMirror type) {
        TypeKind kind = type.getKind();
        if (kind == TypeKind.BOOLEAN) {
            return true;
        }
        if (kind != TypeKind.DECLARED) {
            return false;
        }
        return environment.getTypeUtils().isSameType(
                environment.getTypeUtils().erasure(type),
                environment.getTypeUtils().erasure(type("java.lang.Boolean")));
    }

    boolean hasPrivateRuntimeType(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return hasPrivateRuntimeType(((ArrayType) type).getComponentType());
        }
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        return ((DeclaredType) type).asElement().getModifiers().contains(Modifier.PRIVATE);
    }

    Element runtimeTypeElement(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return runtimeTypeElement(((ArrayType) type).getComponentType());
        }
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        return ((DeclaredType) environment.getTypeUtils().erasure(type)).asElement();
    }

    String objectType(RecordComponentElement component) {
        return environment.getTypeUtils().erasure(component.asType()).toString();
    }

    boolean isDifferentPackage(Element element, Registry registry) {
        String elementPackage = environment.getElementUtils().getPackageOf(element).getQualifiedName().toString();
        return !elementPackage.equals(registry.modulePackage());
    }

    boolean hasPrivateEnclosingType(Element element) {
        Element current = element.getEnclosingElement();
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            if ((current.getKind().isClass() || current.getKind().isInterface())
                    && current.getModifiers().contains(Modifier.PRIVATE)) {
                return true;
            }
            current = current.getEnclosingElement();
        }
        return false;
    }

    boolean isPubliclyAccessible(Element element) {
        Element current = element;
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            if ((current.getKind().isClass() || current.getKind().isInterface())
                    && !current.getModifiers().contains(Modifier.PUBLIC)) {
                return false;
            }
            current = current.getEnclosingElement();
        }
        return true;
    }
}
