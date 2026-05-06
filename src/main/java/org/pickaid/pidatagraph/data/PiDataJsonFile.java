package org.pickaid.pidatagraph.data;

import com.google.gson.JsonElement;
import java.util.Objects;

public record PiDataJsonFile(String path, JsonElement json) {
    public PiDataJsonFile {
        path = checkPath(path);
        json = Objects.requireNonNull(json, "json");
    }

    private static String checkPath(String path) {
        String checked = Objects.requireNonNull(path, "path").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("generated data file path must not be blank");
        }
        if (checked.startsWith("/") || checked.endsWith("/") || checked.contains("..") || !isValidPath(checked)) {
            throw new IllegalArgumentException("invalid generated data file path: " + path);
        }
        return checked;
    }

    private static boolean isValidPath(String path) {
        for (int index = 0; index < path.length(); index++) {
            char next = path.charAt(index);
            if (!(next >= 'a' && next <= 'z')
                    && !(next >= '0' && next <= '9')
                    && next != '_' && next != '-' && next != '.' && next != '/') {
                return false;
            }
        }
        return true;
    }
}
