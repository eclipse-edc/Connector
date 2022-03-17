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
package org.eclipse.dataspaceconnector.dataplane.framework.registry;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Default {@link TransferServiceRegistry} implementation.
 */
public class TransferServiceRegistryImpl implements TransferServiceRegistry {

    private final Collection<TransferService> transferServices = new LinkedHashSet<>();
    private final TransferServiceSelectionStrategy transferServiceSelectionStrategy;

    public TransferServiceRegistryImpl(TransferServiceSelectionStrategy transferServiceSelectionStrategy) {
        this.transferServiceSelectionStrategy = transferServiceSelectionStrategy;
    }

    @Override
    public void registerTransferService(TransferService transferService) {
        transferServices.add(transferService);
    }

    @Override
    @Nullable
    public TransferService resolveTransferService(DataFlowRequest request) {
        var possibleServices = transferServices.stream().filter(s -> s.canHandle(request));
        return transferServiceSelectionStrategy.chooseTransferService(request, possibleServices);
    }
}
