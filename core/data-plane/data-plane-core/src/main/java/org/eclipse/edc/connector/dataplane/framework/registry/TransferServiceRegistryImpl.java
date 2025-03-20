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
 *       Cofinity-X - prioritized transfer services
 *
 */

package org.eclipse.edc.connector.dataplane.framework.registry;

import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;

/**
 * Default {@link TransferServiceRegistry} implementation.
 */
public class TransferServiceRegistryImpl implements TransferServiceRegistry {

    private final Collection<PrioritizedTransferService> transferServices = new LinkedHashSet<>();
    private final TransferServiceSelectionStrategy transferServiceSelectionStrategy;

    public TransferServiceRegistryImpl(TransferServiceSelectionStrategy transferServiceSelectionStrategy) {
        this.transferServiceSelectionStrategy = transferServiceSelectionStrategy;
    }

    @Override
    public void registerTransferService(TransferService transferService) {
        transferServices.add(new PrioritizedTransferService(0, transferService));
    }
    
    @Override
    public void registerTransferService(int priority, TransferService transferService) {
        transferServices.add(new PrioritizedTransferService(priority, transferService));
    }
    
    @Override
    @Nullable
    public TransferService resolveTransferService(DataFlowStartMessage request) {
        var prioritizedServicesPresent = transferServices.stream()
                .map(PrioritizedTransferService::priority)
                .anyMatch(priority -> priority > 0);

        if (prioritizedServicesPresent) {
            return transferServices.stream()
                    .filter(pts -> pts.service.canHandle(request))
                    .sorted(Comparator.comparingInt(pts -> -pts.priority))
                    .map(PrioritizedTransferService::service)
                    .findFirst().orElse(null);
        }

        var possibleServices = transferServices.stream()
                .map(PrioritizedTransferService::service)
                .filter(ts -> ts.canHandle(request));
        return transferServiceSelectionStrategy.chooseTransferService(request, possibleServices);
    }
    
    record PrioritizedTransferService(int priority, TransferService service) { }
}
