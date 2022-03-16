/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer.store;

import org.eclipse.dataspaceconnector.spi.persistence.StateEntityStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Manages persistent storage of {@link TransferProcess} state.
 */
@Feature(TransferProcessStore.FEATURE)
public interface TransferProcessStore extends StateEntityStore<TransferProcess> {
    String FEATURE = "edc:core:transfer:transferprocessstore";

    /**
     * Returns the transfer process for the id or null if not found.
     */
    @Nullable
    TransferProcess find(String id);

    /**
     * Returns the transfer process for the data request id or null if not found.
     */
    @Nullable
    String processIdForTransferId(String id);

    /**
     * Creates a transfer process.
     */
    void create(TransferProcess process);

    /**
     * Updates a transfer process.
     */
    void update(TransferProcess process);

    /**
     * Deletes a transfer process.
     */
    void delete(String processId);

    /**
     * Returns all the transfer processes in the store that are covered by a given {@link QuerySpec}.
     * <p>
     * Note: supplying a sort field that does not exist on the {@link TransferProcess} may cause some implementations
     * to return an empty Stream, others will return an unsorted Stream, depending on the backing storage implementation.
     */
    Stream<TransferProcess> findAll(QuerySpec querySpec);
}
