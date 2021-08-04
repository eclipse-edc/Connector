/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.provision.aws;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Map;

public class AwsTemporarySecretToken implements SecretToken {
    private final String sessionToken;
    private final long expiration;
    private final String accessKeyId;
    private final String secretAccessKey;

    public AwsTemporarySecretToken(@JsonProperty("accessKeyId") String accessKeyId, @JsonProperty("secretAccessKey") String secretAccessKey, @JsonProperty("sessionToken") String sessionToken, @JsonProperty("expiration") long expiration) {
        this.sessionToken = sessionToken;
        this.expiration = expiration;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    @Override
    public long getExpiration() {
        return expiration;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of("accessKeyId", accessKeyId,
                "secretAccessKey", secretAccessKey,
                "token", sessionToken,
                "expiration", expiration);
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }
}
