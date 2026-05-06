package org.pickaid.pidatagraph.processor;

import java.util.List;
import javax.lang.model.element.TypeElement;

final class PiDataGraphProcessorModel {
    private PiDataGraphProcessorModel() {
    }

    record Registry(
            String moduleQualifiedName,
            String moduleSimpleName,
            String modulePackage,
            String modid,
            String fieldName,
            String path,
            String folder,
            String sync
    ) {
    }

    record Input(TypeElement element, Registry registry, String name, String facadeName, List<Component> components) {
        String typeName() {
            return element.getQualifiedName().toString();
        }
    }

    record Output(TypeElement element, Registry registry, String name, List<OutputComponent> components) {
    }

    record Component(String key, String accessor, String type, boolean number, boolean referenceNumber) {
    }

    record OutputComponent(String key, String type, OutputKind kind) {
    }

    enum OutputKind {
        NUMBER,
        FLAG,
        OBJECT
    }
}
