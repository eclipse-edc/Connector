package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A temporary token with write privileges to the data destination.
 */
public class DestinationSecretToken {
    private String token;
    private long expiration;

    public DestinationSecretToken(@JsonProperty("token") String token, @JsonProperty("expiration") long expiration) {
        this.token = token;
        this.expiration = expiration;
    }

    public String getToken() {
        return token;
    }

    public long getExpiration() {
        return expiration;
    }
}
