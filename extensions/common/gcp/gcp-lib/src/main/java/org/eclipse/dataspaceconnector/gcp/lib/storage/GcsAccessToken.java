/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.lib.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Map;

@JsonTypeName("dataspaceconnector:gcstoken")
public class GcsAccessToken implements SecretToken {
    private final String token;
    private final long expiration;

    public GcsAccessToken(@JsonProperty("token") String writeOnlyToken, @JsonProperty("expiration") long expiration) {
        token = writeOnlyToken;
        this.expiration = expiration;
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
        return Map.of("token", token,
                "expiration", expiration);
    }
}
