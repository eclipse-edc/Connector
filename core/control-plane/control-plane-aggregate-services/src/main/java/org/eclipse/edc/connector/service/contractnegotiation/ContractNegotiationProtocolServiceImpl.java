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
 *       Fraunhofer Institute for Software and Systems Engineering - implementation for provider offer
 *
 */

package org.eclipse.edc.connector.service.contractnegotiation;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.service.protocol.BaseProtocolService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;

public class ContractNegotiationProtocolServiceImpl extends BaseProtocolService implements ContractNegotiationProtocolService {

    private final ContractNegotiationStore store;
    private final TransactionContext transactionContext;
    private final ContractValidationService validationService;
    private final ContractNegotiationObservable observable;
    private final Monitor monitor;
    private final Telemetry telemetry;

    public ContractNegotiationProtocolServiceImpl(ContractNegotiationStore store,
                                                  TransactionContext transactionContext,
                                                  ContractValidationService validationService,
                                                  IdentityService identityService,
                                                  PolicyEngine policyEngine,
                                                  ContractNegotiationObservable observable,
                                                  Monitor monitor, Telemetry telemetry) {
        super(identityService, policyEngine, monitor);
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
    public ServiceResult<ContractNegotiation> notifyRequested(ContractRequestMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> validateOffer(message, claimToken))
                .compose(validatedOffer -> {
                    var result = message.getProviderPid() == null
                            ? createNegotiation(message, validatedOffer)
                            : getNegotiation(message.getProviderPid());

                    return result.onSuccess(negotiation -> {
                        if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                            return;
                        }
                        negotiation.protocolMessageReceived(message.getId());
                        negotiation.addContractOffer(validatedOffer.getOffer());
                        negotiation.transitionRequested();
                        update(negotiation);
                        observable.invokeForEach(l -> l.requested(negotiation));
                    });
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyOffered(ContractOfferMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> getNegotiation(message.getProcessId())
                        .compose(negotiation -> validateRequest(claimToken, negotiation))
                )
                .onSuccess(negotiation -> {
                    if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return;
                    }
                    negotiation.protocolMessageReceived(message.getId());
                    negotiation.addContractOffer(message.getContractOffer());
                    negotiation.transitionOffered();
                    update(negotiation);
                    observable.invokeForEach(l -> l.offered(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAccepted(ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> getNegotiation(message.getProcessId())
                        .compose(negotiation -> validateRequest(claimToken, negotiation))
                )
                .onSuccess(negotiation -> {
                    if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return;
                    }
                    negotiation.protocolMessageReceived(message.getId());
                    negotiation.transitionAccepted();
                    update(negotiation);
                    observable.invokeForEach(l -> l.accepted(negotiation));
                }));

    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAgreed(ContractAgreementMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> getNegotiation(message.getProcessId())
                        .compose(negotiation -> validateAgreed(message, claimToken, negotiation))
                )
                .onSuccess(negotiation -> {
                    if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return;
                    }
                    negotiation.protocolMessageReceived(message.getId());
                    negotiation.setContractAgreement(message.getContractAgreement());
                    negotiation.transitionAgreed();
                    update(negotiation);
                    observable.invokeForEach(l -> l.agreed(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyVerified(ContractAgreementVerificationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> getNegotiation(message.getProcessId())
                        .compose(negotiation -> validateRequest(claimToken, negotiation))
                )
                .onSuccess(negotiation -> {
                    if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return;
                    }
                    negotiation.protocolMessageReceived(message.getId());
                    negotiation.transitionVerified();
                    update(negotiation);
                    observable.invokeForEach(l -> l.verified(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyFinalized(ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> getNegotiation(message.getProcessId())
                        .compose(negotiation -> validateRequest(claimToken, negotiation))
                )
                .onSuccess(negotiation -> {
                    if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return;
                    }
                    negotiation.protocolMessageReceived(message.getId());
                    negotiation.transitionFinalized();
                    update(negotiation);
                    observable.invokeForEach(l -> l.finalized(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyTerminated(ContractNegotiationTerminationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .compose(claimToken -> getNegotiation(message.getProcessId())
                        .compose(negotiation -> validateRequest(claimToken, negotiation))
                )
                .onSuccess(negotiation -> {
                    if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return;
                    }
                    negotiation.protocolMessageReceived(message.getId());
                    negotiation.transitionTerminated();
                    update(negotiation);
                    observable.invokeForEach(l -> l.terminated(negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> findById(String id, TokenRepresentation tokenRepresentation) {
        return verifyToken(tokenRepresentation).compose(claimToken -> transactionContext.execute(() -> Optional.ofNullable(store.findById(id))
                .map(negotiation -> validateRequest(claimToken, negotiation))
                .orElse(ServiceResult.notFound("No negotiation with id %s found".formatted(id)))));
    }

    @NotNull
    private ServiceResult<ContractNegotiation> createNegotiation(ContractRequestMessage message, ValidatedConsumerOffer validatedOffer) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(message.getConsumerPid())
                .counterPartyId(validatedOffer.getConsumerIdentity())
                .counterPartyAddress(message.getCallbackAddress())
                .protocol(message.getProtocol())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(PROVIDER)
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

    private ServiceResult<ContractNegotiation> getNegotiation(String negotiationId) {
        return store.findByIdAndLease(negotiationId)
                // recover needed to maintain backward compatibility when there was no distinction between providerPid and consumerPid
                .recover(it -> store.findByCorrelationIdAndLease(negotiationId))
                .flatMap(ServiceResult::from);
    }

    private void update(ContractNegotiation negotiation) {
        store.save(negotiation);
        monitor.debug(() -> "[%s] ContractNegotiation %s is now in state %s."
                .formatted(negotiation.getType(), negotiation.getId(), negotiation.stateAsString()));
    }

}
