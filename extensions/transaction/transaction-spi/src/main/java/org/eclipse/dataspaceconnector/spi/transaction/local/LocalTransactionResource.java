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

package org.eclipse.dataspaceconnector.spi.transaction.local;

/**
 * A resource that can be enlisted in a local transaction.
 */
public interface LocalTransactionResource {

    /**
     * Signals a transaction has started.
     */
    void start();

    /**
     * Signals a transaction has committed.
     */
    void commit();

    /**
     * Signals a transaction has been rolled back.
     */
    void rollback();

}
