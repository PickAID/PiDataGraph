package org.pickaid.pidatagraph.processor;

import javax.lang.model.element.TypeElement;

final class PiDataGraphGeneratedNames {
    private PiDataGraphGeneratedNames() {
    }

    static String moduleName(TypeElement module, String packageName) {
        return moduleName(module.getQualifiedName().toString(), packageName);
    }

    static String moduleName(String qualifiedName, String packageName) {
        String localName = packageName.isEmpty()
                ? qualifiedName
                : qualifiedName.substring(packageName.length() + 1);
        return localName.replace('.', '_') + "_PiDataGraph";
    }

    static String inputPrefix(String name) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char next = name.charAt(index);
            if (Character.isLetterOrDigit(next)) {
                if (Character.isUpperCase(next) && index > 0) {
                    builder.append('_');
                }
                builder.append(Character.toUpperCase(next));
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    static String binderName(String name) {
        StringBuilder builder = new StringBuilder();
        boolean upper = true;
        for (int index = 0; index < name.length(); index++) {
            char next = name.charAt(index);
            if (!Character.isLetterOrDigit(next)) {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(next) : next);
            upper = false;
        }
        return builder + "Binder";
    }
}
