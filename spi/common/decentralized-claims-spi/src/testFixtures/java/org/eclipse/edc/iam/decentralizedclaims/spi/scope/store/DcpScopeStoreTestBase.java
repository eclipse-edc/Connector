/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi.scope.store;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DcpScopeStoreTestBase {

    /**
     * Create a fresh store instance for each test.
     * <p>
     * Implementations must ensure that the returned store is isolated (clean state).
     */
    protected abstract DcpScopeStore getStore();

    @Test
    void save() {
        var store = getStore();

        var scope = DcpScope.Builder.newInstance()
                .id("scope-1")
                .value("value-1")
                .profile("profile-a")
                .build();

        var saveResult = store.save(scope);
        assertTrue(saveResult.succeeded(), "save should succeed");

        var queryResult = store.query(QuerySpec.Builder.newInstance().build());
        assertTrue(queryResult.succeeded(), "query should succeed");
        var scopes = Objects.requireNonNull(queryResult.getContent());
        assertTrue(scopes.stream().anyMatch(s -> "scope-1".equals(s.getId())), "saved scope must be returned by query");
    }

    @Test
    void delete() {
        var store = getStore();

        var scope = DcpScope.Builder.newInstance()
                .id("scope-to-delete")
                .value("v")
                .profile("p")
                .build();

        var save = store.save(scope);
        assertTrue(save.succeeded(), "save should succeed before delete");

        var delete = store.delete(scope.getId());
        assertTrue(delete.succeeded(), "delete should succeed");

        var queryResult = store.query(QuerySpec.Builder.newInstance().build());
        assertTrue(queryResult.succeeded(), "query should succeed after delete");
        var scopes = Objects.requireNonNull(queryResult.getContent());
        assertFalse(scopes.stream().anyMatch(s -> scope.getId().equals(s.getId())), "deleted scope must not be returned by query");
    }

    @Test
    void query_withDefaultType() {
        var store = getStore();

        var scope = DcpScope.Builder.newInstance()
                .id("scope-1")
                .value("value-1")
                .profile("profile-a")
                .build();

        var saveResult = store.save(scope);
        assertTrue(saveResult.succeeded(), "save should succeed");

        var queryResult = store.query(QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("type", "=", "DEFAULT"))
                .build());
        assertTrue(queryResult.succeeded(), "query should succeed");
        var scopes = Objects.requireNonNull(queryResult.getContent());
        assertTrue(scopes.stream().anyMatch(s -> "scope-1".equals(s.getId())), "saved scope must be returned by query");
    }

    @Test
    void query_withPolicyType() {
        var store = getStore();

        var scope = DcpScope.Builder.newInstance()
                .id("scope-1")
                .value("value-1")
                .profile("profile-a")
                .type(DcpScope.Type.POLICY)
                .prefixMapping("prefix-mapping-1")
                .build();

        var saveResult = store.save(scope);
        assertTrue(saveResult.succeeded(), "save should succeed");

        var queryResult = store.query(QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("type", "=", "POLICY"))
                .build());
        assertTrue(queryResult.succeeded(), "query should succeed");
        var scopes = Objects.requireNonNull(queryResult.getContent());
        assertTrue(scopes.stream().anyMatch(s -> "scope-1".equals(s.getId())), "saved scope must be returned by query");
    }

}