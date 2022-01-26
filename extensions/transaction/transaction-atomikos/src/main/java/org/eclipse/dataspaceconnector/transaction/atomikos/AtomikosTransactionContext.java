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
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;

/**
 * An implementation backed by the Atomikos transaction manager.
 */
public class AtomikosTransactionContext implements TransactionContext {
    private TransactionManager transactionManager;
    private Monitor monitor;

    public AtomikosTransactionContext(Monitor monitor) {
        this.monitor = monitor;
    }

    public void initialize(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void execute(TransactionBlock block) {
        var startedTransaction = false;
        Transaction transaction = null;
        try {
            transaction = transactionManager.getTransaction();
            if (transaction == null) {
                transactionManager.begin();
                transaction = transactionManager.getTransaction();
                startedTransaction = true;
            }

            block.execute();

        } catch (Exception e) {
            try {
                if (transaction != null) {
                    transaction.setRollbackOnly();
                }
            } catch (SystemException ex) {
                monitor.severe("Error setting rollback", ex);
            }
        } finally {
            if (startedTransaction) {
                try {
                    var status = transactionManager.getStatus();
                    if (STATUS_ACTIVE == status) {
                        transactionManager.commit();
                    } else if (STATUS_MARKED_ROLLBACK == status) {
                        transactionManager.rollback();
                    }
                } catch (HeuristicRollbackException | SystemException | HeuristicMixedException | RollbackException e) {
                    monitor.severe("Transaction error", e);
                }
            }
        }
    }
}
