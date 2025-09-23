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
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * An implementation of {@link EventSubscriber} which listen to {@link TransferProcessEvent} specifically
 * the {@link TransferProcessStarted}, {@link TransferProcessCompleted}, {@link TransferProcessTerminated} and
 * update the {@link EndpointDataReferenceStore} accordingly
 */
public class EndpointDataReferenceStoreReceiver implements EventSubscriber {

    private final EndpointDataReferenceStore dataReferenceStore;
    private final PolicyArchive policyArchive;
    private final ContractAgreementService contractAgreementService;
    private final TransactionContext transactionContext;
    private final Monitor monitor;
    private final Map<Class<? extends TransferProcessEvent>, Function<? extends TransferProcessEvent, Result<Void>>> handlers = new HashMap<>();

    public EndpointDataReferenceStoreReceiver(EndpointDataReferenceStore dataReferenceStore, PolicyArchive policyArchive, ContractAgreementService contractAgreementService, TransactionContext transactionContext, Monitor monitor) {
        this.dataReferenceStore = dataReferenceStore;
        this.policyArchive = policyArchive;
        this.contractAgreementService = contractAgreementService;
        this.transactionContext = transactionContext;
        this.monitor = monitor;
        registerHandler(TransferProcessStarted.class, this::handleTransferStarted);
        registerHandler(TransferProcessTerminated.class, this::handleTransferTerminated);
        registerHandler(TransferProcessSuspended.class, this::handleTransferSuspended);
        registerHandler(TransferProcessCompleted.class, this::handleTransferCompleted);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof TransferProcessEvent transferProcessEvent && transferProcessEvent.getType().equals(TransferProcess.Type.CONSUMER.name())) {
            var handler = (Function<E, Result<Void>>) handlers.get(transferProcessEvent.getClass());
            if (handler != null) {
                transactionContext.execute(() -> {
                    handler.apply(event.getPayload())
                            .onFailure(failure -> monitor.severe("Failed to process event %s: %s".formatted(event.getPayload().getClass().getSimpleName(), failure.getFailureDetail())))
                            .orElseThrow(failure -> new EdcException("Failed to process event %s: %s".formatted(event.getPayload().getClass().getSimpleName(), failure.getFailureDetail())));
                });
            }
        }
    }

    private <T extends TransferProcessEvent> void registerHandler(Class<T> klass, Function<T, Result<Void>> function) {
        handlers.put(klass, function);
    }

    private Result<Void> handleTransferStarted(TransferProcessStarted transferStarted) {

        if (transferStarted.getDataAddress() != null) {
            var contractNegotiationId = Optional.ofNullable(contractAgreementService.findNegotiation(transferStarted.getContractId()))
                    .map(ContractNegotiation::getId)
                    .orElse(null);

            if (contractNegotiationId == null) {
                var msg = "Contract Negotiation for transfer process %s not found. The EDR cached entry will not have an associated contract negotiation id";
                monitor.debug(msg.formatted(transferStarted.getTransferProcessId()));
            }

            var policy = policyArchive.findPolicyForContract(transferStarted.getContractId());

            if (policy == null) {
                var msg = "Policy associated to the transfer process %s and contract agreement %s not found";
                return Result.failure(msg.formatted(transferStarted.getTransferProcessId(), transferStarted.getContractId()));
            }

            var result = dataReferenceStore.save(toEndpointDataReferenceEntry(transferStarted, policy.getAssigner(), contractNegotiationId), transferStarted.getDataAddress());

            if (result.failed()) {
                return Result.failure(result.getFailureDetail());
            }
        }
        return Result.success();
    }

    private EndpointDataReferenceEntry toEndpointDataReferenceEntry(TransferProcessStarted transferProcessStarted, String providerId, String contractNegotiationId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .id(transferProcessStarted.getContractId())
                .transferProcessId(transferProcessStarted.getTransferProcessId())
                .assetId(transferProcessStarted.getAssetId())
                .contractNegotiationId(contractNegotiationId)
                .providerId(providerId)
                .agreementId(transferProcessStarted.getContractId())
                .participantContextId(transferProcessStarted.getParticipantContextId())
                .build();
    }

    private Result<Void> handleTransferTerminated(TransferProcessTerminated transferProcessTerminated) {
        return removeCachedEdr(transferProcessTerminated.getTransferProcessId());
    }

    private Result<Void> handleTransferSuspended(TransferProcessSuspended transferProcessSuspended) {
        return removeCachedEdr(transferProcessSuspended.getTransferProcessId());
    }

    private Result<Void> handleTransferCompleted(TransferProcessCompleted transferProcessCompleted) {
        return removeCachedEdr(transferProcessCompleted.getTransferProcessId());
    }

    private Result<Void> removeCachedEdr(String transferProcessId) {
        if (dataReferenceStore.findById(transferProcessId) != null) {
            var result = dataReferenceStore.delete(transferProcessId);
            if (result.failed()) {
                return Result.failure(result.getFailureDetail());
            }
        }
        return Result.success();
    }
}
