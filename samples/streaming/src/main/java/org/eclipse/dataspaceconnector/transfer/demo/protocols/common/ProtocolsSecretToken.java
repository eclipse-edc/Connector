/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Map;

/**
 *
 */
@JsonTypeName("dataspaceconnector:protocolssecrettoken")
public class ProtocolsSecretToken implements SecretToken {
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

