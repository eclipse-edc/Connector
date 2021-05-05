/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A temporary token with write privileges to the data destination.
 */
public class DestinationSecretToken {
    public static final String KEY = "dagx-destination-token";

    private final String token;
    private final long expiration;
    private final String accessKeyId;
    private final String secretAccessKey;

    public DestinationSecretToken(@JsonProperty("accessKeyId") String accessKeyId, @JsonProperty("secretAccessKey") String secretAccessKey, @JsonProperty("token") String token, @JsonProperty("expiration") long expiration) {
        this.token = token;
        this.expiration = expiration;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    public String getToken() {
        return token;
    }

    public long getExpiration() {
        return expiration;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }
}
