package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;

class PiEngineDocumentationSnippetTest {
    private static final Pattern JSON_BLOCK = Pattern.compile("```json\\R(.*?)\\R```", Pattern.DOTALL);
    private static final Pattern TYPE_HEADING = Pattern.compile("^#### `(pidatagraph:[^`]+)`", Pattern.MULTILINE);
    private static final Pattern TABLE_TYPE = Pattern.compile("^\\| `(pidatagraph:[^`]+)`", Pattern.MULTILINE);

    @Test
    void actionDocsUseDecodableActionAndPredicateJsonSnippets() throws IOException {
        assertActionDocumentation("docs/wiki/1.20.1/zhCN/actions.md");
        assertActionDocumentation("docs/wiki/1.20.1/enUS/actions.md");
    }

    private static void assertActionDocumentation(String path) throws IOException {
        String markdown = Files.readString(Path.of(path));
        PiEngineActionRegistry actionRegistry = PiEngineActionRegistry.standard();

        List<String> coreActions = PiEngineActions.core().stream()
                .map(type -> type.id().toString())
                .toList();
        assertEquals(coreActions, documentedTableTypes(section(markdown, actionTableMarker(markdown), "\n### Action JSON")),
                path + " action table types");

        String actionFormatSection = section(markdown, "### Action JSON", "\n### ");
        List<String> actionHeadings = documentedTypeHeadings(actionFormatSection);
        assertEquals(coreActions, actionHeadings, path + " documented action types");

        List<JsonElement> actionSnippets = jsonBlocks(actionFormatSection);
        assertEquals(14, actionSnippets.size(), path + " action json snippet count");
        for (int index = 0; index < actionSnippets.size(); index++) {
            int snippet = index + 1;
            var action = actionRegistry.codec()
                    .parse(JsonOps.INSTANCE, actionSnippets.get(index))
                    .getOrThrow(false, message -> {
                        throw new AssertionError(path + " action json snippet " + snippet + ": " + message);
                    });
            assertEquals(actionHeadings.get(index), action.type().id().toString(),
                    path + " action json snippet " + snippet + " type");
        }

        List<JsonElement> actionChainSnippets = jsonBlocks(section(markdown, actionChainMarker(markdown), "\n## "));
        assertEquals(1, actionChainSnippets.size(), path + " action chain json snippet count");
        actionRegistry.codec()
                .parse(JsonOps.INSTANCE, actionChainSnippets.get(0))
                .getOrThrow(false, message -> {
                    throw new AssertionError(path + " action chain json snippet: " + message);
                });

        String predicateFormatSection = section(markdown, "### Predicate JSON", "\n## ");
        List<String> corePredicates = PiEnginePredicates.core().stream()
                .map(type -> type.id().toString())
                .filter(id -> !id.equals("pidatagraph:expression"))
                .toList();
        assertEquals(corePredicates, documentedTableTypes(section(markdown, predicateTableMarker(markdown), "\n### Predicate JSON")),
                path + " predicate table types");

        List<String> predicateHeadings = documentedTypeHeadings(predicateFormatSection);
        assertEquals(corePredicates, predicateHeadings, path + " documented predicate types");

        List<JsonElement> predicateSnippets = jsonBlocks(predicateFormatSection);
        assertEquals(15, predicateSnippets.size(), path + " predicate json snippet count");
        for (int index = 0; index < predicateSnippets.size(); index++) {
            int snippet = index + 1;
            var predicate = PiEnginePredicates.standardCodec()
                    .parse(JsonOps.INSTANCE, predicateSnippets.get(index))
                    .getOrThrow(false, message -> {
                        throw new AssertionError(path + " predicate json snippet " + snippet + ": " + message);
                    });
            String expectedType = index == 0 ? "pidatagraph:expression" : predicateHeadings.get(index - 1);
            assertEquals(expectedType, predicate.type().id().toString(),
                    path + " predicate json snippet " + snippet + " type");
        }
    }

    private static String section(String markdown, String startMarker, String nextTopLevelMarker) {
        int start = markdown.indexOf(startMarker);
        if (start < 0) {
            throw new AssertionError("missing section marker: " + startMarker);
        }
        int end = markdown.indexOf(nextTopLevelMarker, start + startMarker.length());
        if (end < 0) {
            throw new AssertionError("missing next section marker after: " + startMarker);
        }
        return markdown.substring(start, end);
    }

    private static String actionChainMarker(String markdown) {
        return markdown.contains("### 完整 Action Chain 示例")
                ? "### 完整 Action Chain 示例"
                : "### Complete Action Chain Example";
    }

    private static String actionTableMarker(String markdown) {
        return markdown.contains("## 内置 Action") ? "## 内置 Action" : "## Core Actions";
    }

    private static String predicateTableMarker(String markdown) {
        return markdown.contains("## 内置 Predicate") ? "## 内置 Predicate" : "## Core Predicates";
    }

    private static List<JsonElement> jsonBlocks(String markdown) {
        return JSON_BLOCK.matcher(markdown)
                .results()
                .map(MatchResult::group)
                .map(block -> block.substring(block.indexOf('\n') + 1, block.lastIndexOf('\n')))
                .map(JsonParser::parseString)
                .toList();
    }

    private static List<String> documentedTypeHeadings(String markdown) {
        return TYPE_HEADING.matcher(markdown)
                .results()
                .map(result -> result.group(1))
                .toList();
    }

    private static List<String> documentedTableTypes(String markdown) {
        return TABLE_TYPE.matcher(markdown)
                .results()
                .map(result -> result.group(1))
                .toList();
    }
}
