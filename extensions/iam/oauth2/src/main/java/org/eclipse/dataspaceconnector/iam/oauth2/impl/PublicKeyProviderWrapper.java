/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.auth0.jwt.interfaces.RSAKeyProvider;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Shim from an {@link PublicKeyResolver} to a {@link RSAKeyProvider} required by the JWT verifier.
 */
public class PublicKeyProviderWrapper implements RSAKeyProvider {
    private PublicKeyResolver resolver;

    public PublicKeyProviderWrapper(PublicKeyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public RSAPublicKey getPublicKeyById(String id) {
        return resolver.resolveKey(id);
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return null;
    }

    @Override
    public String getPrivateKeyId() {
        return null;
    }
}
