package org.eclipse.dataspaceconnector.ids.daps.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccessTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("scope")
    @JsonSerialize(using = ScopeJsonSerializer.class)
    @JsonDeserialize(using = ScopeJsonDeserializer.class)
    private List<String> scope;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public static class ScopeJsonDeserializer extends StdDeserializer<List<String>> {
        private static final String SCOPE_DELIMITER = " ";

        protected ScopeJsonDeserializer() {
            super(List.class);
        }

        @Override
        public List<String> deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            final String value = p.getValueAsString();

            if (value != null) {
                return Arrays.stream(value.split(SCOPE_DELIMITER))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList());
            }

            return new LinkedList<>();
        }
    }

    public static class ScopeJsonSerializer extends StdSerializer<List<String>> {
        private static final String SCOPE_DELIMITER = " ";

        protected ScopeJsonSerializer() {
            super(String.class, false);
        }

        @Override
        public void serialize(final List<String> value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
            if (value != null) {
                final String scopes = value.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(SCOPE_DELIMITER));

                gen.writeString(scopes);
            }
        }
    }
}
