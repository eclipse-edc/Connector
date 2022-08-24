/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.provision.http.webhook;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Map;

/**
 * Implementation of the {@link SecretToken} that is based on a simple String.
 * This could be an API key, a JWT or any other format serialized to String (e.g. JSON).
 */
public class SimpleSecretToken implements SecretToken {


    private final String token;

    public SimpleSecretToken(String base64SerializedToken) {
        token = base64SerializedToken;
    }

    @Override
    public long getExpiration() {
        return 0;
    }

    @Override
    public Map<String, ?> flatten() {
        return Map.of("token", token);
    }
}
