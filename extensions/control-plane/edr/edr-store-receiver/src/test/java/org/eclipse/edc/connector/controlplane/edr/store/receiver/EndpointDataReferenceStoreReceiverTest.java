/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.edr.store.receiver;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessDeprovisioned;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessDeprovisioningRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessPreparationRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessPrepared;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EndpointDataReferenceStoreReceiverTest {

    private final PolicyArchive policyArchive = mock();
    private final ContractAgreementService contractAgreementService = mock();
    private final EndpointDataReferenceStore dataReferenceStore = mock();

    private final Monitor monitor = mock();

    private EndpointDataReferenceStoreReceiver receiver;

    @BeforeEach
    void setup() {
        receiver = new EndpointDataReferenceStoreReceiver(dataReferenceStore, policyArchive, contractAgreementService, new NoopTransactionContext(), monitor);
    }

    @Test
    void transferStarted_shouldStoreTheEdr() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        var event = TestFunctions.baseBuilder(TransferProcessStarted.Builder.newInstance()).dataAddress(dataAddress).build();

        var policy = mock(Policy.class);
        var contractNegotiation = mock(ContractNegotiation.class);

        when(policyArchive.findPolicyForContract(event.getContractId())).thenReturn(policy);
        when(policy.getAssigner()).thenReturn("providerId");
        when(contractAgreementService.findNegotiation(event.getContractId())).thenReturn(contractNegotiation);
        when(contractNegotiation.getId()).thenReturn("contractNegotiationId");
        when(dataReferenceStore.save(any(), eq(dataAddress))).thenReturn(StoreResult.success());

        receiver.on(TestFunctions.envelopeFor(event));

        var capturedEntry = ArgumentCaptor.forClass(EndpointDataReferenceEntry.class);
        verify(dataReferenceStore).save(capturedEntry.capture(), eq(dataAddress));

        var entry = capturedEntry.getValue();
        assertThat(entry.getTransferProcessId()).isEqualTo(event.getTransferProcessId());
        assertThat(entry.getAgreementId()).isEqualTo(event.getContractId());
        assertThat(entry.getAssetId()).isEqualTo(event.getAssetId());
        assertThat(entry.getProviderId()).isEqualTo("providerId");
        assertThat(entry.getContractNegotiationId()).isEqualTo("contractNegotiationId");

    }

    @Test
    void transferStarted_shouldStoreTheEdr_whenContractNegotiationNotFound() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        var event = TestFunctions.baseBuilder(TransferProcessStarted.Builder.newInstance()).dataAddress(dataAddress).build();

        var policy = mock(Policy.class);

        when(policyArchive.findPolicyForContract(event.getContractId())).thenReturn(policy);
        when(policy.getAssigner()).thenReturn("providerId");
        when(contractAgreementService.findNegotiation(event.getContractId())).thenReturn(null);
        when(dataReferenceStore.save(any(), eq(dataAddress))).thenReturn(StoreResult.success());

        receiver.on(TestFunctions.envelopeFor(event));

        var capturedEntry = ArgumentCaptor.forClass(EndpointDataReferenceEntry.class);
        verify(dataReferenceStore).save(capturedEntry.capture(), eq(dataAddress));

        var entry = capturedEntry.getValue();
        assertThat(entry.getTransferProcessId()).isEqualTo(event.getTransferProcessId());
        assertThat(entry.getAgreementId()).isEqualTo(event.getContractId());
        assertThat(entry.getAssetId()).isEqualTo(event.getAssetId());
        assertThat(entry.getProviderId()).isEqualTo("providerId");
        assertThat(entry.getContractNegotiationId()).isNull();

    }

    @Test
    void transferStarted_shouldFail_whenPolicyNotFound() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        var event = TestFunctions.baseBuilder(TransferProcessStarted.Builder.newInstance()).dataAddress(dataAddress).build();

        when(policyArchive.findPolicyForContract(event.getContractId())).thenReturn(null);
        when(contractAgreementService.findNegotiation(event.getContractId())).thenReturn(null);

        assertThatThrownBy(() -> receiver.on(TestFunctions.envelopeFor(event))).isInstanceOf(EdcException.class);

        verify(monitor).severe(startsWith("Failed to process event %s".formatted(TransferProcessStarted.class.getSimpleName())));
        verifyNoInteractions(dataReferenceStore);

    }

    @Test
    void transferStarted_shouldFail_whenEdrStoreFails() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        var event = TestFunctions.baseBuilder(TransferProcessStarted.Builder.newInstance()).dataAddress(dataAddress).build();

        var policy = mock(Policy.class);

        when(policyArchive.findPolicyForContract(event.getContractId())).thenReturn(policy);
        when(policy.getAssigner()).thenReturn("providerId");
        when(contractAgreementService.findNegotiation(event.getContractId())).thenReturn(null);
        when(dataReferenceStore.save(any(), eq(dataAddress))).thenReturn(StoreResult.generalError("error"));

        assertThatThrownBy(() -> receiver.on(TestFunctions.envelopeFor(event))).isInstanceOf(EdcException.class);

        verify(monitor).severe(startsWith("Failed to process event %s".formatted(TransferProcessStarted.class.getSimpleName())));
        verify(dataReferenceStore).save(any(), any());

    }

    @ParameterizedTest
    @ArgumentsSource(PurgeEvents.class)
    void transferClosed_shouldRemoveCachedEdr(EventEnvelope<TransferProcessEvent> event) {

        when(dataReferenceStore.findById(event.getPayload().getTransferProcessId())).thenReturn(mock());
        when(dataReferenceStore.delete(event.getPayload().getTransferProcessId())).thenReturn(StoreResult.success(mock()));

        receiver.on(event);

        verify(dataReferenceStore).findById(event.getPayload().getTransferProcessId());
        verify(dataReferenceStore).delete(event.getPayload().getTransferProcessId());

    }

    @ParameterizedTest
    @ArgumentsSource(PurgeEvents.class)
    void transferClosed_shouldThrow_whenDeleteFails(EventEnvelope<TransferProcessEvent> event) {

        when(dataReferenceStore.findById(event.getPayload().getTransferProcessId())).thenReturn(mock());
        when(dataReferenceStore.delete(event.getPayload().getTransferProcessId())).thenReturn(StoreResult.generalError("error"));

        assertThatThrownBy(() -> receiver.on(event)).isInstanceOf(EdcException.class);

        verify(dataReferenceStore).findById(event.getPayload().getTransferProcessId());
        verify(dataReferenceStore).delete(event.getPayload().getTransferProcessId());

    }

    @ParameterizedTest
    @ArgumentsSource(PurgeEvents.class)
    void transferClosed_shouldNotThrow_whenEdrItsNotCached(EventEnvelope<TransferProcessEvent> event) {
        when(dataReferenceStore.findById(event.getPayload().getTransferProcessId())).thenReturn(null);

        receiver.on(event);

        verify(dataReferenceStore).findById(event.getPayload().getTransferProcessId());
        verify(dataReferenceStore, never()).delete(event.getPayload().getTransferProcessId());

    }

    @ParameterizedTest
    @ArgumentsSource(UnsupportedEvents.class)
    void transfer_shouldNotRemoveCachedEdr_whenUnsupportedEvents(EventEnvelope<TransferProcessEvent> event) {

        receiver.on(event);
        verifyNoInteractions(dataReferenceStore);
    }

    private static class PurgeEvents implements ArgumentsProvider {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var eventBuilders = Stream.of(
                    TransferProcessTerminated.Builder.newInstance(),
                    TransferProcessCompleted.Builder.newInstance(),
                    TransferProcessSuspended.Builder.newInstance()
            );

            return eventBuilders
                    .map(it -> TestFunctions.baseBuilder((TransferProcessEvent.Builder) it).build())
                    .map(TestFunctions::envelopeFor)
                    .map(Arguments::of);
        }

    }

    private static class UnsupportedEvents implements ArgumentsProvider {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var eventBuilders = Stream.of(
                    TransferProcessDeprovisioned.Builder.newInstance(),
                    TransferProcessDeprovisioningRequested.Builder.newInstance(),
                    TransferProcessInitiated.Builder.newInstance(),
                    TransferProcessProvisioned.Builder.newInstance(),
                    TransferProcessPrepared.Builder.newInstance(),
                    TransferProcessPreparationRequested.Builder.newInstance(),
                    TransferProcessRequested.Builder.newInstance().transferProcessId("id")
            );

            return eventBuilders
                    .map(it -> TestFunctions.baseBuilder((TransferProcessEvent.Builder) it).build())
                    .map(TestFunctions::envelopeFor)
                    .map(Arguments::of);
        }

    }
}
