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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - implementation for provider offer
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *       Schaeffler AG - GetDspRequest refactor
 *
 */

package org.eclipse.edc.connector.controlplane.services.contractnegotiation;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.ConsumerOfferResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.isFinal;

public class ContractNegotiationProtocolServiceImpl implements ContractNegotiationProtocolService {

    private final ContractNegotiationStore store;
    private final TransactionContext transactionContext;
    private final ContractValidationService validationService;
    private final ConsumerOfferResolver consumerOfferResolver;
    private final ProtocolTokenValidator protocolTokenValidator;
    private final ContractNegotiationObservable observable;
    private final Monitor monitor;
    private final Telemetry telemetry;

    public ContractNegotiationProtocolServiceImpl(ContractNegotiationStore store,
                                                  TransactionContext transactionContext,
                                                  ContractValidationService validationService,
                                                  ConsumerOfferResolver consumerOfferResolver,
                                                  ProtocolTokenValidator protocolTokenValidator,
                                                  ContractNegotiationObservable observable,
                                                  Monitor monitor, Telemetry telemetry) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.validationService = validationService;
        this.consumerOfferResolver = consumerOfferResolver;
        this.protocolTokenValidator = protocolTokenValidator;
        this.observable = observable;
        this.monitor = monitor;
        this.telemetry = telemetry;
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyRequested(ParticipantContext participantContext, ContractRequestMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchValidatableOffer(participantContext, message)
                .compose(validatableOffer -> verifyRequest(participantContext, tokenRepresentation, validatableOffer.getContractPolicy(), message)
                        .compose(agent -> {
                            var result = validationService.validateInitialOffer(agent, validatableOffer);
                            if (result.failed()) {
                                monitor.debug("[Provider] Contract offer rejected as invalid: " + result.getFailureDetail());
                                return ServiceResult.badRequest("Contract offer is not valid: " + result.getFailureDetail());
                            }

                            var offerId = validatableOffer.getOfferId();
                            var contractOffer = ContractOffer.Builder.newInstance()
                                    .id(offerId.toString())
                                    .policy(validatableOffer.getTargetedContractPolicy().toBuilder().type(PolicyType.OFFER).build())
                                    .assetId(offerId.assetIdPart())
                                    .build();

                            if (message.getProviderPid() == null) {
                                var negotiation = createNegotiation(participantContext, message, agent.getIdentity(), PROVIDER, message.getCallbackAddress());
                                return requestedAction(message, negotiation, contractOffer);
                            }

                            return onMessageDo(participantContext, message, agent,
                                    negotiation -> requestedAction(message, negotiation, contractOffer));
                        })
                ));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyOffered(ParticipantContext participantContext, ContractOfferMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyRequest(participantContext, tokenRepresentation, message.getContractOffer().getPolicy(), message)
                .compose(agent -> {
                    if (message.getConsumerPid() == null) {
                        var negotiation = createNegotiation(participantContext, message, agent.getIdentity(), CONSUMER, message.getCallbackAddress());
                        return offeredAction(message, negotiation);
                    }

                    return onMessageDo(participantContext, message, agent,
                            negotiation -> offeredAction(message, negotiation));
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAccepted(ParticipantContext participantContext, ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message))
                .compose(agent -> onMessageDo(participantContext, message, agent, contractNegotiation -> acceptedAction(message, contractNegotiation))));

    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAgreed(ParticipantContext participantContext, ContractAgreementMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message))
                .compose(agent -> onMessageDo(participantContext, message, agent, negotiation -> agreedAction(message, negotiation, agent))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyVerified(ParticipantContext participantContext, ContractAgreementVerificationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message))
                .compose(agent -> onMessageDo(participantContext, message, agent, contractNegotiation -> verifiedAction(message, contractNegotiation, agent))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyFinalized(ParticipantContext participantContext, ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message))
                .compose(agent -> onMessageDo(participantContext, message, agent, contractNegotiation -> finalizedAction(message, contractNegotiation))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyTerminated(ParticipantContext participantContext, ContractNegotiationTerminationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message))
                .compose(agent -> onMessageDo(participantContext, message, agent, contractNegotiation -> terminatedAction(message, contractNegotiation))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> findById(ParticipantContext participantContext, ContractNegotiationRequestMessage message, TokenRepresentation tokenRepresentation) {

        return transactionContext.execute(() -> getNegotiation(participantContext, message.getNegotiationId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> {
                            var result = validationService.validateRequest(agent, contractNegotiation);
                            if (result.failed()) {
                                return ServiceResult.badRequest("Invalid client credentials: " + result.getFailureDetail());
                            }

                            return ServiceResult.success(contractNegotiation);
                        })));
    }

    @NotNull
    private ServiceResult<ContractNegotiation> onMessageDo(ParticipantContext participantContext, ContractRemoteMessage message,
                                                           ParticipantAgent agent, Function<ContractNegotiation, ServiceResult<ContractNegotiation>> action) {
        var leaseNegotiation = store.findByIdAndLease(message.getProcessId());
        if (leaseNegotiation.failed()) {
            return ServiceResult.from(leaseNegotiation.mapFailure());
        }

        var negotiation = leaseNegotiation.getContent();
        if (!negotiation.getParticipantContextId().equals(participantContext.getParticipantContextId())) {
            store.breakLease(negotiation);
            return ServiceResult.notFound("No negotiation with id %s found".formatted(negotiation.getId()));
        }

        var result = validationService.validateRequest(agent, negotiation);
        if (result.failed()) {
            store.breakLease(negotiation);
            return ServiceResult.badRequest("Invalid client credentials: " + result.getFailureDetail());
        }

        if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
            store.breakLease(negotiation);
            return ServiceResult.success(negotiation);
        }

        return action.apply(negotiation)
                .onFailure(f -> store.breakLease(negotiation));
    }

    @NotNull
    private ContractNegotiation createNegotiation(ParticipantContext participantContext, ContractRemoteMessage message, String counterPartyIdentity, ContractNegotiation.Type type, String callbackAddress) {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(message.getConsumerPid())
                .counterPartyId(counterPartyIdentity)
                .counterPartyAddress(callbackAddress)
                .protocol(message.getProtocol())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(type)
                .participantContextId(participantContext.getParticipantContextId())
                .build();
    }

    @NotNull
    private ServiceResult<ValidatableConsumerOffer> fetchValidatableOffer(ParticipantContext participantContext, ContractRequestMessage message) {
        var offerId = message.getContractOffer().getId();

        var result = consumerOfferResolver.resolveOffer(offerId);

        if (result.succeeded() && participantContext.getParticipantContextId().equals(result.getContent().getContractDefinition().getParticipantContextId())) {
            return result;
        }

        if (result.failed()) {
            monitor.debug(() -> "Failed to resolve offer: %s".formatted(result.getFailureDetail()));
        } else {
            monitor.debug(() -> "Offer %s does not belong to participantContext %s".formatted(offerId, participantContext.getParticipantContextId()));
        }

        return ServiceResult.notFound("Not found");
    }

    private ServiceResult<ContractNegotiation> requestedAction(ContractRequestMessage message, ContractNegotiation negotiation, ContractOffer contractOffer) {
        if (negotiation.getType().equals(PROVIDER) && negotiation.canBeRequestedProvider()) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.addContractOffer(contractOffer);
            negotiation.transitionRequested();
            return update(negotiation)
                    .onSuccess(i -> observable.invokeForEach(l -> l.requested(negotiation)))
                    .map(i -> negotiation);
        }

        return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be requested"));
    }

    private ServiceResult<ContractNegotiation> offeredAction(ContractOfferMessage message, ContractNegotiation negotiation) {
        if (negotiation.getType().equals(CONSUMER) && negotiation.canBeOfferedConsumer()) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.addContractOffer(message.getContractOffer());
            negotiation.transitionOffered();
            return update(negotiation)
                    .onSuccess(i -> observable.invokeForEach(l -> l.offered(negotiation)))
                    .map(i -> negotiation);
        }

        return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be offered"));
    }

    @NotNull
    private ServiceResult<ContractNegotiation> acceptedAction(ContractNegotiationEventMessage message, ContractNegotiation negotiation) {
        if (negotiation.canBeAccepted()) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.transitionAccepted();
            update(negotiation);
            observable.invokeForEach(l -> l.accepted(negotiation));
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be accepted"));
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> agreedAction(ContractAgreementMessage message, ContractNegotiation negotiation, ParticipantAgent agent) {
        var agreement = message.getContractAgreement();
        var result = validationService.validateConfirmed(agent, agreement, negotiation.getLastContractOffer());
        if (result.failed()) {
            var msg = "Contract agreement received. Validation failed: " + result.getFailureDetail();
            monitor.debug("[Consumer] " + msg);
            return ServiceResult.badRequest(msg);
        }

        if (negotiation.getType().equals(CONSUMER) && negotiation.canBeAgreedConsumer()) {

            var agreementWithClaims = message.getContractAgreement().toBuilder()
                    .participantContextId(negotiation.getParticipantContextId())
                    .claims(agent.getClaims())
                    .build();

            negotiation.protocolMessageReceived(message.getId());
            negotiation.setContractAgreement(agreementWithClaims);
            negotiation.transitionAgreed();
            update(negotiation);
            observable.invokeForEach(l -> l.agreed(negotiation));
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be agreed"));
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> verifiedAction(ContractAgreementVerificationMessage message, ContractNegotiation negotiation, ParticipantAgent agent) {
        if (negotiation.getType().equals(PROVIDER) && negotiation.canBeVerifiedProvider()) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.transitionVerified();
            var agreementWithClaims = negotiation.getContractAgreement().toBuilder().claims(agent.getClaims()).build();
            negotiation.setContractAgreement(agreementWithClaims);
            update(negotiation);
            observable.invokeForEach(l -> l.verified(negotiation));
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be verified"));
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> finalizedAction(ContractNegotiationEventMessage message, ContractNegotiation negotiation) {
        if (negotiation.getType().equals(CONSUMER) && negotiation.canBeFinalized() && !isFinal(negotiation.getState())) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.transitionFinalized();
            update(negotiation);
            observable.invokeForEach(l -> l.finalized(negotiation));
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be finalized"));
        }

    }

    @NotNull
    private ServiceResult<ContractNegotiation> terminatedAction(ContractNegotiationTerminationMessage message, ContractNegotiation negotiation) {
        if (negotiation.canBeTerminated() && !isFinal(negotiation.getState())) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.transitionTerminated();
            update(negotiation);
            observable.invokeForEach(l -> l.terminated(negotiation));
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be terminated"));
        }
    }

    private ServiceResult<ParticipantAgent> verifyRequest(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, Policy policy, RemoteMessage message) {
        return protocolTokenValidator.verify(participantContext, tokenRepresentation, RequestContractNegotiationPolicyContext::new, policy, message);
    }

    private ServiceResult<ContractNegotiation> getNegotiation(ParticipantContext participantContext, String negotiationId) {
        return Optional.ofNullable(store.findById(negotiationId))
                .filter(cn -> participantContext.getParticipantContextId().equals(cn.getParticipantContextId()))
                .map(ServiceResult::success)
                .orElseGet(() -> ServiceResult.notFound("No negotiation with id %s found".formatted(negotiationId)));
    }

    private ServiceResult<Void> update(ContractNegotiation negotiation) {
        return store.save(negotiation)
                .onSuccess(i -> monitor.debug(() -> "[%s] ContractNegotiation %s is now in state %s."
                        .formatted(negotiation.getType(), negotiation.getId(), negotiation.stateAsString())))
                .flatMap(ServiceResult::from);

    }

}
