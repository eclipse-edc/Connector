/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.provision.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Map;

/**
 * Describes an OAuth2 {@link SecretToken}, that's a field containing the token representation.
 */
public class Oauth2SecretToken implements SecretToken {

    private final String token;

    public Oauth2SecretToken(@JsonProperty("token") String token) {
        this.token = token;
    }

    @Override
    public long getExpiration() {
        return 0;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of();
    }

}
