package org.eclipse.edc.jsonld.transformer.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;

import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.VALUE;

/**
 *
 */
public class ToGenericTypeTransformer extends AbstractJsonLdTransformer<JsonValue, Object> {
    private final ObjectMapper mapper;

    public ToGenericTypeTransformer(ObjectMapper mapper) {
        super(JsonValue.class, Object.class);
        this.mapper = mapper;
    }

    @Override
    public Object transform(JsonValue value, @NotNull TransformerContext context) {
        if (value instanceof JsonObject) {
            var object = (JsonObject) value;
            var valueField = object.get(VALUE);
            if (valueField == null) {
                // parse it as a generic object type
                return toJavaType(object);
            }
            return transform(valueField, context);
        } else if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            return jsonArray.stream().map(entry -> transform(entry, context)).collect(toList());
        } else if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        } else if (value instanceof JsonNumber) {
            return ((JsonNumber) value).doubleValue(); // use to double to avoid loss of precision
        }
        return null;
    }

    private Object toJavaType(JsonObject object) {
        try {
            return mapper.readValue(object.toString(), Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);   // TODO
        }
    }

}
