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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweReader;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweWriter;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.WriteRequestReader;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ErrorResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.WriteResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Default identity hub implementation.
 */
public class IdentityHubImpl implements IdentityHub {
    private final IdentityHubStore store;
    private final Supplier<PrivateKeyWrapper> privateKey;
    private final DidPublicKeyResolver publicKeyResolver;
    private final ObjectMapper objectMapper;

    public IdentityHubImpl(IdentityHubStore store, Supplier<PrivateKeyWrapper> privateKey, DidPublicKeyResolver resolver, ObjectMapper objectMapper) {
        this.store = store;
        this.privateKey = privateKey;
        publicKeyResolver = resolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public String write(String jwe) {
        var commit = new WriteRequestReader().mapper(objectMapper).privateKey(privateKey.get()).verifier((jwso) -> true).jwe(jwe).readCommit();
        store.write(commit);

        var response = WriteResponse.Builder.newInstance().revision(commit.getObjectId()).build();
        return writeResponse(response, commit.getIss());
    }

    @Override
    public void write(Commit commit) {
        store.write(commit);
    }

    @Override
    public String queryCommits(String jwe) {
        var query = new GenericJweReader().mapper(objectMapper).privateKey(privateKey.get()).jwe(jwe).readType(CommitQueryRequest.class);
        var commits = store.query(query.getQuery());
        var response = CommitQueryResponse.Builder.newInstance().commits(new ArrayList<>(commits)).build();
        return writeResponse(response, query.getIss());
    }

    @Override
    public String queryObjects(String jwe) {
        var query = new GenericJweReader().mapper(objectMapper).privateKey(privateKey.get()).jwe(jwe).readType(ObjectQueryRequest.class);
        var hubObjects = store.query(query.getQuery());

        var response = ObjectQueryResponse.Builder.newInstance().objects(new ArrayList<>(hubObjects)).build();
        return writeResponse(response, query.getIss());
    }

    /**
     * writes a response JWE using the public key of the ISS sender
     */
    private String writeResponse(Object response, String iss) {
        var result = publicKeyResolver.resolvePublicKey(iss);
        if (result.invalid()) {
            try {
                return objectMapper.writeValueAsString(new ErrorResponse("500", "Unable to resolve recipient public key: " + result.getInvalidMessage()));
            } catch (JsonProcessingException e) {
                throw new EdcException(e);
            }
        }

        return new GenericJweWriter().objectMapper(objectMapper).privateKey(privateKey.get()).publicKey(result.getWrapper()).payload(response).buildJwe();
    }
}
