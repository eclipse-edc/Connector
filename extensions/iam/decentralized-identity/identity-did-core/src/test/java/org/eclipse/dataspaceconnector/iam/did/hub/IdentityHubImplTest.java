/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.RsaPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.RsaPublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweReader;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweWriter;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.WriteRequestWriter;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.HubObject;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.WriteResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.testfixtures.TemporaryKeyLoader;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class IdentityHubImplTest {
    private IdentityHubImpl hub;
    private IdentityHubStore store;
    private PrivateKeyWrapper privateKey;
    private PublicKeyWrapper publicKey;
    private ObjectMapper objectMapper;

    @Test
    void verifyStore() {
        var jwe = new WriteRequestWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .commitObject(Map.of("foo", "bar"))
                .kid("kid")
                .sub("sub")
                .context("Foo")
                .type("Bar").buildJwe();

        var responseJwe = hub.write(jwe);

        var response = new GenericJweReader().mapper(objectMapper).jwe(responseJwe).privateKey(privateKey).readType(WriteResponse.class);

        assertNotNull(response.getRevisions().get(0));
        verify(store).write(isA(Commit.class));
    }

    @Test
    void verifyCommitQuery() {
        var commit = Commit.Builder.newInstance().context("foo").type("foo").objectId("123").iss("baz").sub("quux").payload("payload").alg("RSA256").kid("kid").build();

        var query = CommitQuery.Builder.newInstance().objectId("123").build();
        var request = CommitQueryRequest.Builder.newInstance().query(query).iss("123").aud("aud").sub("sub").build();
        when(store.query(isA(CommitQuery.class))).thenReturn(List.of(commit));

        var jwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .payload(request)
                .buildJwe();

        var responseJwe = hub.queryCommits(jwe);

        var response = new GenericJweReader().mapper(objectMapper).jwe(responseJwe).privateKey(privateKey).readType(CommitQueryResponse.class);

        assertNotNull(response.getCommits().get(0));
        verify(store).query(isA(CommitQuery.class));
    }

    @Test
    void verifyObjectQuery() {
        var hubObject = HubObject.Builder.newInstance().type("Foo").id("123").createdBy("test").sub("quux").build();

        var query = ObjectQuery.Builder.newInstance().type("Foo").build();
        var request = ObjectQueryRequest.Builder.newInstance().query(query).iss("123").aud("aud").sub("sub").build();
        when(store.query(isA(ObjectQuery.class))).thenReturn(List.of(hubObject));

        var jwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .payload(request)
                .buildJwe();

        var responseJwe = hub.queryObjects(jwe);

        var response = new GenericJweReader().mapper(objectMapper).jwe(responseJwe).privateKey(privateKey).readType(ObjectQueryResponse.class);

        assertThat(response.getObjects()).allMatch(o -> o.getId().equals("123"));
        verify(store).query(isA(ObjectQuery.class));
    }

    @BeforeEach
    void setUp() throws Exception {
        var keys = TemporaryKeyLoader.loadKeys();
        privateKey = new RsaPrivateKeyWrapper(keys.toRSAPrivateKey());
        publicKey = new RsaPublicKeyWrapper(keys.toRSAPublicKey());
        store = mock(IdentityHubStore.class);
        objectMapper = new ObjectMapper();
        hub = new IdentityHubImpl(store, () -> privateKey, did -> Result.success(publicKey), objectMapper);
    }
}
