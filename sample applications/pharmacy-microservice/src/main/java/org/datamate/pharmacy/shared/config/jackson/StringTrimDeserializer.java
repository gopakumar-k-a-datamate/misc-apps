package org.datamate.pharmacy.shared.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

/**
 * Custom Jackson deserializer that automatically trims leading and trailing
 * whitespace from all string values in JSON payloads.
 * Aligned with the dental project's StringTrimDeserializer.
 */
@JsonComponent
public class StringTrimDeserializer extends StdDeserializer<String> {

    public StringTrimDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text != null) {
            return text.trim();
        }
        return null;
    }
}
