package org.eclipse.edc.jsonld.transformer;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.function.BiConsumer;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.KEYWORDS;

/**
 * Methods for navigating JSON-LD structures.
 */
public class JsonLdNavigator {

    /**
     * Traverses properties on a JSON object, skipping {@link JsonLdKeywords#KEYWORDS}.
     */
    public static void visitProperties(JsonObject object, BiConsumer<String, JsonValue> consumer) {
        object.entrySet().stream().filter(entry -> !KEYWORDS.contains(entry.getKey())).forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
    }

    private JsonLdNavigator() {
    }
}
