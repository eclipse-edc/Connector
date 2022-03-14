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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.framework.e2e.EndToEndTest.createRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceRegistryImplTest {
    TransferService transferService = mock(TransferService.class);
    TransferService transferService2 = mock(TransferService.class);

    DataFlowRequest request = createRequest("1").build();
    TransferServiceSelectionStrategy transferServiceSelectionStrategy = mock(TransferServiceSelectionStrategy.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Stream<TransferService>> streamCaptor = ArgumentCaptor.forClass(Stream.class);

    @Test
    void resolveTransferService_filters_matches() {
        // Arrange
        when(transferService.canHandle(request)).thenReturn(false);
        when(transferService2.canHandle(request)).thenReturn(true);

        // Act
        createRegistryAndResolveForRequest();

        // Assert
        assertThat(streamCaptor.getValue()).containsExactly(transferService2);
    }

    @Test
    void resolveTransferService_handles_multipleMatches() {
        // Arrange
        when(transferService.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(true);

        // Act
        createRegistryAndResolveForRequest();

        // Assert
        assertThat(streamCaptor.getValue()).containsExactly(transferService, transferService2);
    }

    @Test
    void resolveTransferService_handles_noMatch() {
        // Arrange
        var registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);

        // Act
        registry.resolveTransferService(request);

        // Assert
        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
        assertThat(streamCaptor.getValue()).isEmpty();
    }

    @Test
    void resolveTransferService_returns_strategyResult() {
        // Arrange
        var registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
        when(transferServiceSelectionStrategy.chooseTransferService(eq(request), any()))
                .thenReturn(transferService);

        // Act
        TransferService resolved = registry.resolveTransferService(request);

        // Assert
        assertThat(resolved).isSameAs(transferService);
    }

    private void createRegistryAndResolveForRequest() {
        // Arrange
        var registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
        registry.registerTransferService(transferService);
        registry.registerTransferService(transferService2);

        // Act
        registry.resolveTransferService(request);

        // Assert
        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
    }
}
