package com.example.sonata_brain.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Prepares request and response bodies for the log: secrets masked, size capped (REQ-008.AC4). */
public final class LogBodies {

    static final int MAX_CHARS = 64 * 1024;
    private static final Set<String> SECRET_KEYS = Set.of("password", "token", "accesstoken", "refreshtoken");
    private static final ObjectMapper JSON = new ObjectMapper();

    private LogBodies() {
    }

    /** JSON with secret values replaced by "***"; anything that isn't JSON is kept as is. Always capped. */
    public static String prepare(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        String result = body;
        try {
            JsonNode tree = JSON.readTree(body);
            if (tree != null && (tree.isObject() || tree.isArray())) {
                mask(tree);
                result = JSON.writeValueAsString(tree);
            }
        } catch (Exception notJson) {
            // keep the raw text
        }
        return result.length() <= MAX_CHARS ? result : result.substring(0, MAX_CHARS) + "…[truncated]";
    }

    private static void mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SECRET_KEYS.contains(field.getKey().toLowerCase())) {
                    field.setValue(object.textNode("***"));
                } else {
                    mask(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(LogBodies::mask);
        }
    }
}
