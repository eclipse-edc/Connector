package org.eclipse.dataspaceconnector.spi.pagination;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

public class StringCursor implements Cursor {
    private final String marker;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public StringCursor(@NotNull String marker) {
        Objects.requireNonNull(marker);
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }

    @Override
    public String getValue() {
        try {
            byte[] data = OBJECT_MAPPER.writeValueAsBytes(new CursorSerialization(marker));
            return Base64.getEncoder().encodeToString(data);
        } catch (JsonProcessingException ignore) {
            return null;
        }
    }

    public static @Nullable StringCursor fromValue(String representation) {
        try {
            byte[] data = Base64.getDecoder().decode(representation);
            CursorSerialization cursorSerialization = OBJECT_MAPPER.readValue(data, CursorSerialization.class);
            return new StringCursor(cursorSerialization.marker);
        } catch (IOException ignore) {
            return null;
        }
    }

    private static class CursorSerialization {
        @JsonProperty
        private final String marker;

        @JsonCreator
        private CursorSerialization(String marker) {
            this.marker = marker;
        }
    }
}
