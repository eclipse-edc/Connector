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

package org.eclipse.edc.connector.dataplane.framework.registry;

import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceRegistryImplTest {
    TransferService transferService = mock(TransferService.class);
    TransferService transferService2 = mock(TransferService.class);

    DataFlowStartMessage request = createRequest().build();
    TransferServiceSelectionStrategy transferServiceSelectionStrategy = mock(TransferServiceSelectionStrategy.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Stream<TransferService>> streamCaptor = ArgumentCaptor.forClass(Stream.class);

    @Test
    void resolveTransferService_filters_matches() {
        when(transferService.canHandle(request)).thenReturn(false);
        when(transferService2.canHandle(request)).thenReturn(true);

        createRegistryAndResolveForRequest();

        assertThat(streamCaptor.getValue()).containsExactly(transferService2);
    }

    @Test
    void resolveTransferService_handles_multipleMatches() {
        when(transferService.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(true);

        createRegistryAndResolveForRequest();

        assertThat(streamCaptor.getValue()).containsExactly(transferService, transferService2);
    }

    @Test
    void resolveTransferService_handles_noMatch() {
        var registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);

        registry.resolveTransferService(request);

        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
        assertThat(streamCaptor.getValue()).isEmpty();
    }

    @Test
    void resolveTransferService_returns_strategyResult() {
        var registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
        when(transferServiceSelectionStrategy.chooseTransferService(eq(request), any()))
                .thenReturn(transferService);

        var resolved = registry.resolveTransferService(request);

        assertThat(resolved).isSameAs(transferService);
    }

    private void createRegistryAndResolveForRequest() {
        var registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
        registry.registerTransferService(transferService);
        registry.registerTransferService(transferService2);

        registry.resolveTransferService(request);

        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
    }

    private DataFlowStartMessage.Builder createRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("any").build());
    }
}
