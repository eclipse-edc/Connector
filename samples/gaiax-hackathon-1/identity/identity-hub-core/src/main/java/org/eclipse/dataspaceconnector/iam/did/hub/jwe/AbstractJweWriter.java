/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Implements base JWE writer functionality.
 */
public abstract class AbstractJweWriter<T extends AbstractJweWriter<?>> {
    protected RSAPrivateKey privateKey;
    protected RSAPublicKey publicKey;
    protected ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public T publicKey(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T privateKey(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return (T) this;
    }

    public abstract String buildJwe();


}
