package com.votify.dto.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acepta categorías como lista de cadenas o como lista de objetos {@code { "name": "..." }}.
 */
public class CategoryNamesDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull() || !node.isArray()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (JsonNode el : node) {
            if (el == null || el.isNull()) {
                continue;
            }
            if (el.isTextual()) {
                out.add(el.asText());
            } else if (el.isObject()) {
                if (el.hasNonNull("name")) {
                    out.add(el.get("name").asText());
                } else if (el.hasNonNull("categoryName")) {
                    out.add(el.get("categoryName").asText());
                } else if (el.hasNonNull("title")) {
                    out.add(el.get("title").asText());
                }
            }
        }
        return out.isEmpty() ? null : out;
    }
}
