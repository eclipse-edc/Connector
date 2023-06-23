/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.service.contractnegotiation;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;

public class ContractNegotiationProtocolServiceImpl implements ContractNegotiationProtocolService {

    private final ContractNegotiationStore store;
    private final TransactionContext transactionContext;
    private final ContractValidationService validationService;
    private final ContractNegotiationObservable observable;
    private final Monitor monitor;
    private final Telemetry telemetry;

    public ContractNegotiationProtocolServiceImpl(ContractNegotiationStore store,
                                                  TransactionContext transactionContext,
                                                  ContractValidationService validationService, ContractNegotiationObservable observable,
                                                  Monitor monitor, Telemetry telemetry) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.validationService = validationService;
        this.observable = observable;
        this.monitor = monitor;
        this.telemetry = telemetry;
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyRequested(ContractRequestMessage message, ClaimToken claimToken) {
        return transactionContext.execute(() -> validateOffer(message, claimToken)
                    .compose(validatedOffer -> createNegotiation(message, validatedOffer))
                    .onSuccess(negotiation -> {
                        monitor.debug(() -> "[Provider] Contract offer received.");
                        negotiation.transitionRequested();
                        update(negotiation);
                        observable.invokeForEach(l -> l.requested(negotiation));
                    }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyOffered(ContractRequestMessage message, ClaimToken claimToken) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAccepted(ContractNegotiationEventMessage message, ClaimToken claimToken) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAgreed(ContractAgreementMessage message, ClaimToken claimToken) {
        return transactionContext.execute(() -> getNegotiation(message)
                .compose(negotiation -> validateAgreed(message, claimToken, negotiation))
                .onSuccess(negotiation -> {
                    monitor.debug("[Consumer] Contract agreement received. Validation successful.");
                    negotiation.setContractAgreement(message.getContractAgreement());
                    negotiation.transitionAgreed();
                    update(negotiation);
                    observable.invokeForEach(l -> l.agreed(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyVerified(ContractAgreementVerificationMessage message, ClaimToken claimToken) {
        return transactionContext.execute(() -> getNegotiation(message)
                .compose(negotiation -> validateRequest(claimToken, negotiation))
                .onSuccess(negotiation -> {
                    negotiation.transitionVerified();
                    update(negotiation);
                    observable.invokeForEach(l -> l.verified(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyFinalized(ContractNegotiationEventMessage message, ClaimToken claimToken) {
        return transactionContext.execute(() -> getNegotiation(message)
                .compose(negotiation -> validateRequest(claimToken, negotiation))
                .onSuccess(negotiation -> {
                    negotiation.transitionFinalized();
                    update(negotiation);
                    observable.invokeForEach(l -> l.finalized(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyTerminated(ContractNegotiationTerminationMessage message, ClaimToken claimToken) {
        return transactionContext.execute(() -> getNegotiation(message)
                .compose(negotiation -> validateRequest(claimToken, negotiation))
                .onSuccess(negotiation -> {
                    negotiation.transitionTerminated();
                    update(negotiation);
                    observable.invokeForEach(l -> l.terminated(negotiation));
                }));
    }
    
    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> findById(String id, ClaimToken claimToken) {
        return transactionContext.execute(() -> Optional.ofNullable(store.findById(id))
                .map(negotiation -> validateGetRequest(claimToken, negotiation))
                .map(ServiceResult::success)
                .orElse(ServiceResult.notFound(format("No negotiation with id %s found", id))));
    }

    @NotNull
    private ServiceResult<ContractNegotiation> createNegotiation(ContractRequestMessage message, ValidatedConsumerOffer validatedOffer) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(message.getProcessId())
                .counterPartyId(validatedOffer.getConsumerIdentity())
                .counterPartyAddress(message.getCallbackAddress())
                .protocol(message.getProtocol())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(PROVIDER)
                .contractOffer(validatedOffer.getOffer())
                .build();

        return ServiceResult.success(negotiation);
    }

    @NotNull
    private ServiceResult<ValidatedConsumerOffer> validateOffer(ContractRequestMessage message, ClaimToken claimToken) {
        var result = message.getContractOffer() != null ?
                validationService.validateInitialOffer(claimToken, message.getContractOffer()) :
                validationService.validateInitialOffer(claimToken, message.getContractOfferId());
        if (result.failed()) {
            monitor.debug("[Provider] Contract offer rejected as invalid: " + result.getFailureDetail());
            return ServiceResult.badRequest("Contract offer is not valid: " + result.getFailureDetail());
        } else {
            return ServiceResult.success(result.getContent());
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> validateAgreed(ContractAgreementMessage message, ClaimToken claimToken, ContractNegotiation negotiation) {
        var agreement = message.getContractAgreement();
        var result = validationService.validateConfirmed(claimToken, agreement, negotiation.getLastContractOffer());
        if (result.failed()) {
            var msg = "Contract agreement received. Validation failed: " + result.getFailureDetail();
            monitor.debug("[Consumer] " + msg);
            return ServiceResult.badRequest(msg);
        } else {
            return ServiceResult.success(negotiation);
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> validateRequest(ClaimToken claimToken, ContractNegotiation negotiation) {
        var result = validationService.validateRequest(claimToken, negotiation);
        if (result.failed()) {
            return ServiceResult.badRequest("Invalid client credentials: " + result.getFailureDetail());
        } else {
            return ServiceResult.success(negotiation);
        }
    }
    
    private ContractNegotiation validateGetRequest(ClaimToken claimToken, ContractNegotiation negotiation) {
        var result = validationService.validateRequest(claimToken, negotiation);
        if (result.failed()) {
            return null;
        } else {
            return negotiation;
        }
    }

    private ServiceResult<ContractNegotiation> getNegotiation(ContractRemoteMessage message) {
        var processId = message.getProcessId();
        var negotiation = store.findForCorrelationId(processId);
        if (negotiation == null) {
            return ServiceResult.notFound(format("ContractNegotiation with processId %s not found", processId));
        } else {
            return ServiceResult.success(negotiation);
        }
    }

    private void update(ContractNegotiation negotiation) {
        store.save(negotiation);
        monitor.debug(String.format("[%s] ContractNegotiation %s is now in state %s.",
                negotiation.getType(), negotiation.getId(), ContractNegotiationStates.from(negotiation.getState())));
    }

}
