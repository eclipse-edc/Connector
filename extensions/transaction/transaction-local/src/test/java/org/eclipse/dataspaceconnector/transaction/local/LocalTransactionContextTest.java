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
package org.eclipse.dataspaceconnector.transaction.local;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transaction.local.LocalTransactionResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LocalTransactionContextTest {
    private LocalTransactionContext transactionContext;
    private LocalTransactionResource dsResource;

    @Test
    void verifyTransaction() {
        // executed a transaction block
        transactionContext.execute(() -> {
        });

        // start and commit should only be called
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).commit();
    }

    @Test
    void verifyJoinNestedTransaction() {
        // executed a nested transaction block
        transactionContext.execute(() -> transactionContext.execute(() -> {
        }));

        // start and commit should only be called once since the nexted trx joins the parent context
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).commit();
    }

    @Test
    void verifyRollbackTransaction() {
        // executed a transaction block
        transactionContext.execute(() -> {
            throw new RuntimeException();
        });

        // start and rollback should only be called once
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).rollback();
    }

    @Test
    void verifyRollbackNestedTransaction() {
        // executed a nested transaction block
        transactionContext.execute(() -> transactionContext.execute(() -> {
            throw new RuntimeException();
        }));

        // start and rollback should only be called once since the nexted trx joins the parent context
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).rollback();
    }

    @Test
    void verifyMultipleResourceEnlistmentCommit() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        transactionContext.execute(() -> {
        });

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).commit();
        verify(dsResource2, times(1)).commit();
    }

    @Test
    void verifyMultipleResourceEnlistmentRollback() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        transactionContext.execute(() -> {
            throw new RuntimeException();
        });

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).rollback();
        verify(dsResource2, times(1)).rollback();
    }

    @Test
    void verifyMultipleResourceEnlistmentFailureCommit() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        doThrow(new RuntimeException()).when(dsResource).commit();

        transactionContext.execute(() -> {
        });

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).commit();
        verify(dsResource2, times(1)).commit();  // ensure commit was called on resource after the exception was thrown
    }

    @Test
    void verifyMultipleResourceEnlistmentFailureRollback() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        doThrow(new RuntimeException()).when(dsResource).rollback();

        transactionContext.execute(() -> {
            throw new RuntimeException();
        });

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).rollback();
        verify(dsResource2, times(1)).rollback();  // ensure commit was called on resource after the exception was thrown
    }

    @BeforeEach
    void setUp() {
        transactionContext = new LocalTransactionContext(mock(Monitor.class));
        dsResource = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource);
    }
}
