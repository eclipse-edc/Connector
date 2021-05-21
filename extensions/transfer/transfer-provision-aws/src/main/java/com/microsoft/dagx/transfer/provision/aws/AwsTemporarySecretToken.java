/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.aws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.transfer.SecretToken;

import java.util.Map;

@JsonTypeName("dagx:awssecrettoken")
public class AwsTemporarySecretToken implements SecretToken {
    private final String token;
    private final long expiration;
    private final String accessKeyId;
    private final String secretAccessKey;

    public AwsTemporarySecretToken(@JsonProperty("accessKeyId") String accessKeyId, @JsonProperty("secretAccessKey") String secretAccessKey, @JsonProperty("token") String token, @JsonProperty("expiration") long expiration) {
        this.token = token;
        this.expiration = expiration;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    public String getToken() {
        return token;
    }

    @Override
    public long getExpiration() {
        return expiration;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of("accessKeyId", accessKeyId,
                "secretAccessKey", secretAccessKey,
                "token", token,
                "expiration", expiration);
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }
}
