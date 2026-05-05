package org.pickaid.pidatagraph.data;

import com.google.gson.JsonElement;
import java.util.Objects;

public record PiDataJsonFile(String path, JsonElement json) {
    public PiDataJsonFile {
        path = Objects.requireNonNull(path, "path");
        json = Objects.requireNonNull(json, "json");
    }
}
