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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.iam.oauth2.identity;

public class IdentityProviderKeyResolverConfiguration {
    private final String jwksUrl;
    private final long keyRefreshInterval;

    public IdentityProviderKeyResolverConfiguration(String jwksUrl, long keyRefreshInterval) {
        this.jwksUrl = jwksUrl;
        this.keyRefreshInterval = keyRefreshInterval;
    }

    public String getJwksUrl() {
        return jwksUrl;
    }

    public long getKeyRefreshInterval() {
        return keyRefreshInterval;
    }
}
