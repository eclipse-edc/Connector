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
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

/**
 * Implements base JWE writer functionality.
 */
public abstract class AbstractJweWriter<T extends AbstractJweWriter<?>> {
    protected PrivateKeyWrapper privateKey;
    protected PublicKeyWrapper publicKey;
    protected ObjectMapper objectMapper;

    public T publicKey(PublicKeyWrapper publicKey) {
        this.publicKey = publicKey;
        return (T) this;
    }

    public T privateKey(PrivateKeyWrapper privateKey) {
        this.privateKey = privateKey;
        return (T) this;
    }

    public T objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return (T) this;
    }

    public abstract String buildJwe();


}
