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

package org.eclipse.edc.gcp.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.connector.transfer.spi.types.SecretToken;

@JsonTypeName("dataspaceconnector:gcptoken")
public class GcpAccessToken implements SecretToken {
    private final String token;
    private final long expiration;

    public GcpAccessToken(@JsonProperty("token") String writeOnlyToken, @JsonProperty("expiration") long expiration) {
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

}
