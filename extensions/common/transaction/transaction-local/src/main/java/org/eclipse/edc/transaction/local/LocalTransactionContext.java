/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation and others
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Daimler TSS GmbH - wrap and re-throw handled exceptions
 *
 */

package org.eclipse.edc.transaction.local;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transaction.spi.local.LocalTransactionContextManager;
import org.eclipse.edc.transaction.spi.local.LocalTransactionResource;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Implements a transaction context for local resources. The purpose of this implementation is to provide a portable transaction programming model for code that executes in
 * environments where a proper JTA transaction manager is not available.
 * <p>
 * Note that this transaction context cannot implement atomicity if multiple resources are enlisted for a transaction. The only way to achieve this is to use XA transactions.
 */
public class LocalTransactionContext implements TransactionContext, LocalTransactionContextManager {
    private final List<LocalTransactionResource> resources = new ArrayList<>();
    private final ThreadLocal<Transaction> transactions = new ThreadLocal<>();

    private final Monitor monitor;

    public LocalTransactionContext(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void registerSynchronization(TransactionSynchronization sync) {
        var transaction = transactions.get();
        if (transaction == null) {
            throw new EdcException("Error registering transaction synchronization: a transaction is not active");
        }
        transaction.registerSynchronization(sync);
    }

    @Override
    public void execute(TransactionBlock block) {
        execute((ResultTransactionBlock<Void>) () -> {
            block.execute();
            return null;
        });
    }

    @Override
    public <T> T execute(ResultTransactionBlock<T> block) {
        var startedTransaction = false;
        var transaction = transactions.get();

        try {
            if (transaction == null) {
                transaction = new Transaction();
                resources.forEach(LocalTransactionResource::start);
                startedTransaction = true;
                transactions.set(transaction);
            }
            return block.execute();
        } catch (Exception e) {
            assert transaction != null;
            transaction.setRollbackOnly();
            if (e instanceof EdcException) {
                throw (EdcException) e;
            }
            throw new EdcException(e.getMessage(), e);
        } finally {
            if (startedTransaction) {
                // notify syncs before resources are called
                transaction.getSynchronizations().forEach(TransactionSynchronization::beforeCompletion);
                if (transaction.isRollbackOnly()) {
                    resources.forEach(localTransactionResource -> {
                        try {
                            localTransactionResource.rollback();
                        } catch (Exception e) {
                            monitor.severe("Error rolling back resource", e);
                        }
                    });
                } else {
                    resources.forEach(localTransactionResource -> {
                        try {
                            localTransactionResource.commit();
                        } catch (Exception e) {
                            monitor.severe("Error committing resource", e);
                        }
                    });
                }
                transactions.remove();
            }
        }
    }

    @Override
    public void registerResource(LocalTransactionResource resource) {
        resources.add(resource);
    }


    private static class Transaction {
        private boolean rollbackOnly = false;
        private List<TransactionSynchronization> synchronizations;  // lazy instantiate the collection to avoid object creation if not needed

        boolean isRollbackOnly() {
            return rollbackOnly;
        }

        void setRollbackOnly() {
            rollbackOnly = true;
        }

        List<TransactionSynchronization> getSynchronizations() {
            return synchronizations == null ? emptyList() : synchronizations;
        }

        void registerSynchronization(TransactionSynchronization sync) {
            if (synchronizations == null) {
                synchronizations = new ArrayList<>();
            }
            synchronizations.add(sync);
        }
    }
}
