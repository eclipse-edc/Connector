/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.transaction.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for TransactionContext, to be used only for testing/sampling purposes
 */
public class NoopTransactionContext implements TransactionContext {
    private ThreadLocal<List<TransactionSynchronization>> synchronizations = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void execute(TransactionBlock block) {
        block.execute();
        notifyAndClearSyncs();
    }

    @Override
    public <T> T execute(ResultTransactionBlock<T> block) {
        var result = block.execute();
        notifyAndClearSyncs();
        return result;
    }

    @Override
    public void registerSynchronization(TransactionSynchronization sync) {
        synchronizations.get().add(sync);
    }

    private void notifyAndClearSyncs() {
        var syncList = synchronizations.get();
        syncList.forEach(TransactionSynchronization::beforeCompletion);
        syncList.clear();
    }

}
