package org.pickaid.pidatagraph.processor;

import java.util.LinkedHashSet;
import java.util.List;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphProcessorNames {
    private PiDataGraphProcessorNames() {
    }

    static boolean isValidNamespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            char next = value.charAt(index);
            if (!(next >= 'a' && next <= 'z')
                    && !(next >= '0' && next <= '9')
                    && next != '_' && next != '-' && next != '.') {
                return false;
            }
        }
        return true;
    }

    static boolean isValidPath(String value) {
        for (int index = 0; index < value.length(); index++) {
            char next = value.charAt(index);
            if (!(next >= 'a' && next <= 'z')
                    && !(next >= '0' && next <= '9')
                    && next != '_' && next != '-' && next != '.' && next != '/') {
                return false;
            }
        }
        return true;
    }

    static boolean isValidFolder(String value) {
        return !value.startsWith("/")
                && !value.endsWith("/")
                && !value.contains("..")
                && isValidPath(value);
    }

    static String registrySuggestions(List<Registry> registries) {
        boolean useQualifiedNames = hasDuplicateSimpleSuggestion(registries);
        StringBuilder message = new StringBuilder();
        for (int index = 0; index < registries.size(); index++) {
            if (index > 0) {
                message.append(index == registries.size() - 1 ? " or " : ", ");
            }
            Registry registry = registries.get(index);
            message.append('`').append(useQualifiedNames ? registry.moduleQualifiedName() : registry.moduleSimpleName()).append('.')
                    .append(registry.fieldName()).append('`');
        }
        return message.toString();
    }

    private static boolean hasDuplicateSimpleSuggestion(List<Registry> registries) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (Registry registry : registries) {
            if (!labels.add(registry.moduleSimpleName() + "." + registry.fieldName())) {
                return true;
            }
        }
        return false;
    }

}
