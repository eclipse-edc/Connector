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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceRegistryImplTest {
    private TransferService transferService = mock(TransferService.class);
    private TransferService transferService2 = mock(TransferService.class);
    
    private DataFlowStartMessage request = createRequest().build();
    private TransferServiceSelectionStrategy transferServiceSelectionStrategy = mock(TransferServiceSelectionStrategy.class);
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Stream<TransferService>> streamCaptor = ArgumentCaptor.forClass(Stream.class);
    
    private TransferServiceRegistryImpl registry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
    
    @Test
    void resolveTransferService_noServicesRegistered_shouldReturnNull() {
        var service = registry.resolveTransferService(request);
        
        assertThat(service).isNull();
    }
    
    @Test
    void resolveTransferService_noServiceCanHandle_shouldReturnNull() {
        registry.registerTransferService(transferService);
        when(transferService.canHandle(request)).thenReturn(false);
        
        var service = registry.resolveTransferService(request);
        
        assertThat(service).isNull();
    }
    
    @Test
    void resolveTransferService_withPriorities_shouldReturnHighestPriorityService() {
        registry.registerTransferService(transferService);
        registry.registerTransferService(1, transferService2);
        when(transferService.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(true);
        
        var service = registry.resolveTransferService(request);
        
        assertThat(service).isEqualTo(transferService2);
    }
    
    @Test
    void resolveTransferService_withSamePriority_shouldReturnFirstWithHighestPriority() {
        registry.registerTransferService(1, transferService);
        registry.registerTransferService(1, transferService2);
        when(transferService.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(true);
        
        var service = registry.resolveTransferService(request);
        
        assertThat(service).isEqualTo(transferService);
    }
    
    @Test
    void resolveTransferService_noPriorityAndNoneCanHandle_shouldApplyStrategyWithEmptyStream() {
        registry.registerTransferService(transferService);
        registry.registerTransferService(transferService2);
        when(transferService.canHandle(request)).thenReturn(false);
        when(transferService2.canHandle(request)).thenReturn(false);
        
        registry.resolveTransferService(request);
        
        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
        assertThat(streamCaptor.getValue()).isEmpty();
    }
    
    @Test
    void resolveTransferService_noPriorityAndOneCanHandle_shouldApplyStrategyWithOneService() {
        registry.registerTransferService(transferService);
        registry.registerTransferService(transferService2);
        when(transferService.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(false);
        
        registry.resolveTransferService(request);
        
        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
        assertThat(streamCaptor.getValue()).containsExactly(transferService);
    }
    
    @Test
    void resolveTransferService_noPriorityAndAllCanHandle_shouldApplyStrategyWithAllServices() {
        registry.registerTransferService(transferService);
        registry.registerTransferService(transferService2);
        when(transferService.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(true);
        
        registry.resolveTransferService(request);
        
        verify(transferServiceSelectionStrategy).chooseTransferService(eq(request), streamCaptor.capture());
        assertThat(streamCaptor.getValue()).containsExactly(transferService, transferService2);
    }

    private DataFlowStartMessage.Builder createRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("any").build());
    }
}
