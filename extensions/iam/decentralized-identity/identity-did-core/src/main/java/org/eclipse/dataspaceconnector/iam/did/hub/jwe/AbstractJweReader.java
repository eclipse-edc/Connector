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

/**
 * Implements base JWE reader functionality.
 */
public class AbstractJweReader<T extends AbstractJweReader<?>> {
    protected String jwe;
    protected PrivateKeyWrapper privateKey;
    protected ObjectMapper mapper;

    public T jwe(String jwe) {
        this.jwe = jwe;
        return (T) this;
    }

    public T privateKey(PrivateKeyWrapper privateKey) {
        this.privateKey = privateKey;
        return (T) this;
    }

    public T mapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return (T) this;
    }

}
