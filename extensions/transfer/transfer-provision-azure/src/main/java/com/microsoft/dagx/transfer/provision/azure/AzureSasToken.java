/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.transfer.SecretToken;

import java.util.Map;

@JsonTypeName("dagx:azuretoken")
public class AzureSasToken implements SecretToken {
    private final String sas;
    private final long expiration;

    public AzureSasToken(@JsonProperty("sas") String writeOnlySas, @JsonProperty("expiration") long expiration) {
        sas = writeOnlySas;
        this.expiration = expiration;
    }

    public String getSas() {
        return sas;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of("sas", sas,
                "expiration", expiration);
    }

    @Override
    public long getExpiration() {
        return expiration;
    }
}
