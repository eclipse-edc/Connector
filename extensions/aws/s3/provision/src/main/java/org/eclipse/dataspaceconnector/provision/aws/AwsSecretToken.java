/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.provision.aws;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Map;

public class AwsSecretToken implements SecretToken {
    private final String accessKeyId;
    private final String secretAccessKey;

    public AwsSecretToken(@JsonProperty("accessKeyId") String accessKeyId, @JsonProperty("secretAccessKey") String secretAccessKey) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    @Override
    public long getExpiration() {
        return 0;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of("accessKeyId", accessKeyId,
                "secretAccessKey", secretAccessKey,
                "expiration", getExpiration());
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }
}
