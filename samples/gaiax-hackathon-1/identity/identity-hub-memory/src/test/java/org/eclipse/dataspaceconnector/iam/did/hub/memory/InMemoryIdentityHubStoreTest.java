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
package org.eclipse.dataspaceconnector.iam.did.hub.memory;

import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.ObjectQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
class InMemoryIdentityHubStoreTest {
    private InMemoryIdentityHubStore store;

    @Test
    void verifyCreateAndQuery() {
        Commit commit = createCommit();

        store.write(commit);

        var commitResults = store.query(CommitQuery.Builder.newInstance().objectId("123").build());
        Assertions.assertEquals(commit.getObjectId(),commitResults.iterator().next().getObjectId());

        var objectResults =  store.query(ObjectQuery.Builder.newInstance().context("foo").type("foo").build());
        Assertions.assertEquals(commit.getObjectId(),objectResults.iterator().next().getId());
    }

    @Test
    void verifyQualifiedType() {
        Commit commit = createCommit();

        store.write(commit);

        var objectResults =  store.query(ObjectQuery.Builder.newInstance().context("anothercontext").type("foo").build());
        Assertions.assertTrue(objectResults.isEmpty());
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryIdentityHubStore();
    }

    private Commit createCommit() {
        return Commit.Builder.newInstance().context("foo").type("foo").objectId("123").iss("baz").sub("quux").payload("payload").alg("RSA256").kid("kid").build();
    }

}
