/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.InitiateTransferCommand;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InitiateTransferCommandHandlerTest {

    private final PolicyArchive policyArchive = mock();
    private final TransferProcessStore store = mock();
    private final DataAddressStore dataAddressStore = mock();
    private final TransferProcessListener listener = mock();
    private final Clock clock = mock();
    private final Telemetry telemetry = mock();
    private final Monitor monitor = mock();
    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final InitiateTransferCommandHandler handler = new InitiateTransferCommandHandler(policyArchive, store,
            dataAddressStore, observable, clock, telemetry, monitor);

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
    }

    @Test
    void shouldStoreTransferProcessAndDataAddress() {
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().target("assetId").build());
        when(store.save(any())).thenReturn(StoreResult.success());
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").events(Set.of("test")).build();
        var dataplaneMetadata = DataplaneMetadata.Builder.newInstance().label("label").build();
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        var transferRequest = TransferRequest.Builder.newInstance()
                .dataDestination(dataAddress)
                .callbackAddresses(List.of(callback))
                .dataplaneMetadata(dataplaneMetadata)
                .build();
        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("id")
                .identity("identity")
                .build();
        when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());
        var command = new InitiateTransferCommand(participantContext, transferRequest);

        var result = handler.handle(command);

        assertThat(result).isSucceeded().isNotNull();
        var captor = ArgumentCaptor.forClass(TransferProcess.class);
        verify(store).save(captor.capture());
        var transferProcess = captor.getValue();
        assertThat(transferProcess.getId()).isEqualTo(command.getEntityId());
        assertThat(transferProcess.getCorrelationId()).isNull();
        assertThat(transferProcess.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
        assertThat(transferProcess.getAssetId()).isEqualTo("assetId");
        assertThat(transferProcess.getDataplaneMetadata()).isSameAs(dataplaneMetadata);
        assertThat(transferProcess.getDataDestination()).isNull();
        verify(listener).initiated(any());
        verify(dataAddressStore).store(dataAddress, transferProcess);
    }

    @Test
    void shouldNotStoreDataAddress_whenItsNotProvided() {
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().target("assetId").build());
        when(store.save(any())).thenReturn(StoreResult.success());
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").events(Set.of("test")).build();
        var dataplaneMetadata = DataplaneMetadata.Builder.newInstance().label("label").build();
        var transferRequest = TransferRequest.Builder.newInstance()
                .callbackAddresses(List.of(callback))
                .dataplaneMetadata(dataplaneMetadata)
                .build();
        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("id")
                .identity("identity")
                .build();

        var result = handler.handle(new InitiateTransferCommand(participantContext, transferRequest));

        assertThat(result).isSucceeded().isNotNull();
        verifyNoInteractions(dataAddressStore);
    }

    @Test
    void shouldFail_whenPolicyNotAvailable() {
        when(policyArchive.findPolicyForContract(any())).thenReturn(null);

        var transferRequest = TransferRequest.Builder.newInstance()
                .contractId("contractId")
                .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                .build();

        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participantContextId").identity("id").build();
        var result = handler.handle(new InitiateTransferCommand(participantContext, transferRequest));

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenDataAddressStorageFails() {
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().target("assetId").build());
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").events(Set.of("test")).build();
        var dataplaneMetadata = DataplaneMetadata.Builder.newInstance().label("label").build();
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        var transferRequest = TransferRequest.Builder.newInstance()
                .dataDestination(dataAddress)
                .callbackAddresses(List.of(callback))
                .dataplaneMetadata(dataplaneMetadata)
                .build();
        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("id")
                .identity("identity")
                .build();
        when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.generalError("error"));

        var result = handler.handle(new InitiateTransferCommand(participantContext, transferRequest));

        assertThat(result).isFailed();
        verifyNoInteractions(listener);
        verify(store, never()).save(any());
    }

}
