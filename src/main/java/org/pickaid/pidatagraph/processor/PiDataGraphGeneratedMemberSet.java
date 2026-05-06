package org.pickaid.pidatagraph.processor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Component;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Input;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphGeneratedMemberSet {
    private PiDataGraphGeneratedMemberSet() {
    }

    static Map<String, Set<String>> inputMembers(List<Input> inputs) {
        Map<String, Set<String>> members = new LinkedHashMap<>();
        for (Input input : inputs) {
            Set<String> names = members.computeIfAbsent(registryKey(input.registry()), ignored -> new LinkedHashSet<>());
            String prefix = PiDataGraphGeneratedNames.inputPrefix(input.name());
            names.add(prefix);
            names.add(prefix + "_BINDER");
            for (Component component : input.components()) {
                names.add(component(prefix, component.key()));
            }
        }
        return members;
    }

    static String component(String prefix, String key) {
        return prefix + "_" + PiDataGraphGeneratedNames.inputPrefix(key);
    }

    static String registryKey(Registry registry) {
        return registry.moduleQualifiedName() + "." + registry.fieldName();
    }
}
