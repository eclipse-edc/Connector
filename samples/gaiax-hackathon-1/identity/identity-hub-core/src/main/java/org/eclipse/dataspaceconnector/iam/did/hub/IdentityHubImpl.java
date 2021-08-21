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
package org.eclipse.dataspaceconnector.iam.did.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweReader;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.WriteRequestReader;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.CommitQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.ObjectQueryRequest;

import java.security.interfaces.RSAPrivateKey;
import java.util.function.Supplier;

/**
 * Default identity hub implementation.
 */
public class IdentityHubImpl implements IdentityHub {
    private IdentityHubStore store;
    private Supplier<RSAPrivateKey> privateKey;
    private ObjectMapper objectMapper;

    public IdentityHubImpl(IdentityHubStore store, Supplier<RSAPrivateKey> privateKey, ObjectMapper objectMapper) {
        this.store = store;
        this.privateKey = privateKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public String write(String jwe) {
        // TODO implement permissions
        // TODO implement verification
        var commit = new WriteRequestReader().mapper(objectMapper).privateKey(privateKey.get()).verifier((jwso) -> true).jwe(jwe).readCommit();
        store.write(commit);
        // TODO write JWE using ISS public key
        return null;
    }

    @Override
    public String queryCommits(String jwe) {
        // TODO implement permissions
        var query = new GenericJweReader().mapper(objectMapper).privateKey(privateKey.get()).jwe(jwe).readType(CommitQueryRequest.class);
        var response = store.query(query.getQuery());
        // TODO write JWE using ISS public key
        return null;
    }

    @Override
    public String queryObjects(String jwe) {
        // TODO implement permissions
        var query = new GenericJweReader().mapper(objectMapper).privateKey(privateKey.get()).jwe(jwe).readType(ObjectQueryRequest.class);
        var response = store.query(query.getQuery());
        // TODO write JWE using ISS public key
        return null;
    }
}
