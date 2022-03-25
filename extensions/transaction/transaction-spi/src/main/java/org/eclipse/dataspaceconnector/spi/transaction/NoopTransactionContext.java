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

package org.eclipse.dataspaceconnector.spi.transaction;

/**
 * Default implementation for TransactionContext, to be used only for testing/sampling purposes
 */
public class NoopTransactionContext implements TransactionContext {
    @Override
    public void execute(TransactionBlock block) {
        block.execute();
    }

    @Override
    public <T> T execute(ResultTransactionBlock<T> block) {
        return block.execute();
    }
}
