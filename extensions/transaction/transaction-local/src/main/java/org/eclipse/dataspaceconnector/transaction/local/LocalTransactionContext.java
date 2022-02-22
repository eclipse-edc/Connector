/*
 *  Copyright (c) 2021-2022 Microsoft Corporation and others
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
 */
package org.eclipse.dataspaceconnector.transaction.local;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.local.LocalTransactionContextManager;
import org.eclipse.dataspaceconnector.spi.transaction.local.LocalTransactionResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a transaction context for local resources. The purpose of this implementation is to provide a portable transaction programming model for code that executes in
 * environments where a proper JTA transaction manager is not available.
 * <p>
 * Note that this transaction context cannot implement atomicity if multiple resources are enlisted for a transaction. The only way to achieve this is to use XA transactions.
 */
public class LocalTransactionContext implements TransactionContext, LocalTransactionContextManager {
    private List<LocalTransactionResource> resources = new ArrayList<>();
    private ThreadLocal<Transaction> transactions = new ThreadLocal<>();

    private Monitor monitor;

    public LocalTransactionContext(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void execute(TransactionBlock block) {
        var startedTransaction = false;
        var transaction = transactions.get();

        try {
            if (transaction == null) {
                transaction = new Transaction();
                resources.forEach(LocalTransactionResource::start);
                startedTransaction = true;
                transactions.set(transaction);
            }
            block.execute();
        } catch (Exception e) {
            assert transaction != null;
            transaction.setRollbackOnly();
            if (e instanceof EdcException) {
                throw (EdcException) e;
            }
            throw new EdcException(e.getMessage(), e);
        } finally {
            if (startedTransaction) {
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

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        void setRollbackOnly() {
            rollbackOnly = true;
        }
    }
}
