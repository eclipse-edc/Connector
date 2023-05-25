/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.transaction.atomikos;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtomikosTransactionContextTest {
    private AtomikosTransactionContext transactionContext;
    private TransactionManager transactionManager;
    private Transaction transaction;

    @Test
    void verifyCommit() throws Exception {
        when(transactionManager.getTransaction()).thenReturn(null, transaction);

        transactionContext.execute(() -> {
        });

        verify(transactionManager, times(1)).begin();
        verify(transactionManager, times(1)).commit();
    }

    @Test
    void verifyJoinCommit() throws Exception {
        when(transactionManager.getTransaction()).thenReturn(null, transaction);

        transactionContext.execute(() -> {
            transactionContext.execute(() -> {
            });
        });

        verify(transactionManager, times(1)).begin();
        verify(transactionManager, times(1)).commit();
    }

    @Test
    void verifyJoinRollback() throws Exception {
        when(transactionManager.getTransaction()).thenReturn(null, transaction);
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_MARKED_ROLLBACK);

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() ->
                        transactionContext.execute(() -> {
                            transactionContext.execute(() -> {
                                throw new RuntimeException();
                            });
                        }));

        verify(transactionManager, times(1)).begin();
        verify(transactionManager, times(1)).rollback();
    }

    @Test
    void verifyRollback() throws Exception {
        when(transactionManager.getTransaction()).thenReturn(null, transaction);
        when(transactionManager.getStatus()).thenReturn(Status.STATUS_MARKED_ROLLBACK);
        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() ->
                        transactionContext.execute(() -> {
                            throw new RuntimeException();
                        }));

        verify(transaction, times(1)).setRollbackOnly();
        verify(transactionManager, times(1)).rollback();
    }

    @Test
    void verifySynchronization() throws Exception {
        var sync = mock(TransactionContext.TransactionSynchronization.class);

        when(transactionManager.getTransaction()).thenReturn(null, transaction);

        // the sync should be invoked
        transactionContext.execute(() -> transactionContext.registerSynchronization(sync));

        // the sync should be cleared and should not be invoked again
        transactionContext.execute(() -> {
        });

        verify(transaction, times(1)).registerSynchronization(isA(Synchronization.class));
    }

    @BeforeEach
    void setUp() {
        transactionContext = new AtomikosTransactionContext(mock(Monitor.class));
        transactionManager = mock(TransactionManager.class);
        transactionContext.initialize(transactionManager);
        transaction = mock(Transaction.class);
    }
}
