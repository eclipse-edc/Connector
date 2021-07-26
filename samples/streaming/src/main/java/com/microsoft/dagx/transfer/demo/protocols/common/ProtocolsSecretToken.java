package com.microsoft.dagx.transfer.demo.protocols.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.transfer.SecretToken;

import java.util.Map;

/**
 *
 */
@JsonTypeName("dagx:protocolssecrettoken")
public class ProtocolsSecretToken implements SecretToken{
    private final String token;

    public ProtocolsSecretToken(@JsonProperty("token") String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of("token", token);
    }

    @Override
    public long getExpiration() {
        return Long.MAX_VALUE;
    }
}

