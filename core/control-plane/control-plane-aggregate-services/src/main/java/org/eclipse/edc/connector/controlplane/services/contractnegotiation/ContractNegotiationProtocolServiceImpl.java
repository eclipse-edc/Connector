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
 *       Schaeffler AG
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
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatedConsumerOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.model.Policy;
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
                        .compose(agent -> validateOffer(agent, validatableOffer))
                        .compose(validatedOffer -> {
                            var result = message.getProviderPid() == null
                                    ? createNegotiation(participantContext, message, validatedOffer.getConsumerIdentity(), PROVIDER, message.getCallbackAddress())
                                    : getAndLeaseNegotiation(participantContext, message.getProviderPid());

                            return result.compose(negotiation -> {
                                if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                                    return ServiceResult.success(negotiation);
                                }
                                if (negotiation.getType().equals(PROVIDER) && negotiation.canBeRequestedProvider()) {
                                    negotiation.protocolMessageReceived(message.getId());
                                    negotiation.addContractOffer(validatedOffer.getOffer());
                                    negotiation.transitionRequested();
                                    update(negotiation);
                                    observable.invokeForEach(l -> l.requested(negotiation));
                                    return ServiceResult.success(negotiation);
                                } else {
                                    return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be requested"));
                                }

                            });
                        })
                ));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyOffered(ParticipantContext participantContext, ContractOfferMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyRequest(participantContext, tokenRepresentation, message.getContractOffer().getPolicy(), message)
                .compose(agent -> {
                    ServiceResult<ContractNegotiation> result = message.getConsumerPid() == null
                            ? createNegotiation(participantContext, message, agent.getIdentity(), CONSUMER, message.getCallbackAddress())
                            : getAndLeaseNegotiation(participantContext, message.getConsumerPid())
                            .compose(negotiation -> validateRequest(agent, negotiation).map(it -> negotiation));

                    return result.compose(negotiation -> {
                        if (negotiation.shouldIgnoreIncomingMessage(message.getId())) {
                            return ServiceResult.success(negotiation);
                        }
                        if (negotiation.getType().equals(CONSUMER) && negotiation.canBeOfferedConsumer()) {
                            negotiation.protocolMessageReceived(message.getId());
                            negotiation.addContractOffer(message.getContractOffer());
                            negotiation.transitionOffered();
                            update(negotiation);
                            observable.invokeForEach(l -> l.offered(negotiation));

                            return ServiceResult.success(negotiation);
                        } else {
                            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be offered"));
                        }
                    });
                }));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAccepted(ParticipantContext participantContext, ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> validateRequest(agent, contractNegotiation)))
                .compose(cn -> onMessageDo(participantContext, message, contractNegotiation -> acceptedAction(message, contractNegotiation))));

    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyAgreed(ParticipantContext participantContext, ContractAgreementMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> validateAgreed(message, agent, contractNegotiation)))
                .compose(cn -> onMessageDo(participantContext, message, contractNegotiation -> agreedAction(message, contractNegotiation))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyVerified(ParticipantContext participantContext, ContractAgreementVerificationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> validateRequest(agent, contractNegotiation)))
                .compose(cn -> onMessageDo(participantContext, message, contractNegotiation -> verifiedAction(message, contractNegotiation))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyFinalized(ParticipantContext participantContext, ContractNegotiationEventMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> validateRequest(agent, contractNegotiation)))
                .compose(cn -> onMessageDo(participantContext, message, contractNegotiation -> finalizedAction(message, contractNegotiation))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> notifyTerminated(ParticipantContext participantContext, ContractNegotiationTerminationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> getNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> validateRequest(agent, contractNegotiation)))
                .compose(cn -> onMessageDo(participantContext, message, contractNegotiation -> terminatedAction(message, contractNegotiation))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<ContractNegotiation> findById(ParticipantContext participantContext, ContractRequestMessage message, TokenRepresentation tokenRepresentation) {

        return transactionContext.execute(() -> getNegotiation(participantContext, message.getId())
                .compose(contractNegotiation -> verifyRequest(participantContext, tokenRepresentation, contractNegotiation.getLastContractOffer().getPolicy(), message)
                        .compose(agent -> validateRequest(agent, contractNegotiation)
                                .map(it -> contractNegotiation))));
    }

    @NotNull
    private ServiceResult<ContractNegotiation> onMessageDo(ParticipantContext participantContext, ContractRemoteMessage message, Function<ContractNegotiation, ServiceResult<ContractNegotiation>> action) {
        return getAndLeaseNegotiation(participantContext, message.getProcessId())
                .compose(contractNegotiation -> {
                    if (contractNegotiation.shouldIgnoreIncomingMessage(message.getId())) {
                        return ServiceResult.success(contractNegotiation);
                    } else {
                        return action.apply(contractNegotiation)
                                .onFailure(f -> breakLease(contractNegotiation));
                    }
                });
    }

    @NotNull
    private ServiceResult<ContractNegotiation> createNegotiation(ParticipantContext participantContext, ContractRemoteMessage message, String counterPartyIdentity, ContractNegotiation.Type type, String callbackAddress) {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(message.getConsumerPid())
                .counterPartyId(counterPartyIdentity)
                .counterPartyAddress(callbackAddress)
                .protocol(message.getProtocol())
                .traceContext(telemetry.getCurrentTraceContext())
                .type(type)
                .participantContextId(participantContext.getParticipantContextId())
                .build();

        return ServiceResult.success(negotiation);
    }

    @NotNull
    private ServiceResult<ValidatedConsumerOffer> validateOffer(ParticipantAgent agent, ValidatableConsumerOffer consumerOffer) {
        var result = validationService.validateInitialOffer(agent, consumerOffer);
        if (result.failed()) {
            monitor.debug("[Provider] Contract offer rejected as invalid: " + result.getFailureDetail());
            return ServiceResult.badRequest("Contract offer is not valid: " + result.getFailureDetail());
        } else {
            return ServiceResult.success(result.getContent());
        }
    }

    @NotNull
    private ServiceResult<ValidatableConsumerOffer> fetchValidatableOffer(ParticipantContext participantContext, ContractRequestMessage message) {
        var offerId = message.getContractOffer().getId();

        var result = consumerOfferResolver.resolveOffer(offerId);
        if (result.failed()) {
            monitor.debug(() -> "Failed to resolve offer: %s".formatted(result.getFailureDetail()));
            return ServiceResult.notFound("Not found");
        } else {
            if (participantContext.getParticipantContextId().equals(result.getContent().getContractDefinition().getParticipantContextId())) {
                return result;
            }
            monitor.debug(() -> "Offer %s does not belong to participantContext %s".formatted(offerId, participantContext.getParticipantContextId()));
            return ServiceResult.notFound("Not found");
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> validateAgreed(ContractAgreementMessage message, ParticipantAgent agent, ContractNegotiation negotiation) {
        var agreement = message.getContractAgreement();
        var result = validationService.validateConfirmed(agent, agreement, negotiation.getLastContractOffer());
        if (result.failed()) {
            var msg = "Contract agreement received. Validation failed: " + result.getFailureDetail();
            monitor.debug("[Consumer] " + msg);
            return ServiceResult.badRequest(msg);
        } else {
            return ServiceResult.success(negotiation);
        }
    }

    @NotNull
    private ServiceResult<Void> validateRequest(ParticipantAgent agent, ContractNegotiation negotiation) {
        var result = validationService.validateRequest(agent, negotiation);
        if (result.failed()) {
            return ServiceResult.badRequest("Invalid client credentials: " + result.getFailureDetail());
        } else {
            return ServiceResult.success();
        }
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
    private ServiceResult<ContractNegotiation> agreedAction(ContractAgreementMessage message, ContractNegotiation negotiation) {
        if (negotiation.getType().equals(CONSUMER) && negotiation.canBeAgreedConsumer()) {

            var agreement = message.getContractAgreement().toBuilder()
                    .participantContextId(negotiation.getParticipantContextId())
                    .build();

            negotiation.protocolMessageReceived(message.getId());
            negotiation.setContractAgreement(agreement);
            negotiation.transitionAgreed();
            update(negotiation);
            observable.invokeForEach(l -> l.agreed(negotiation));
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "negotiation cannot be agreed"));
        }
    }

    @NotNull
    private ServiceResult<ContractNegotiation> verifiedAction(ContractAgreementVerificationMessage message, ContractNegotiation negotiation) {
        if (negotiation.getType().equals(PROVIDER) && negotiation.canBeVerifiedProvider()) {
            negotiation.protocolMessageReceived(message.getId());
            negotiation.transitionVerified();
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

    private ServiceResult<ContractNegotiation> getAndLeaseNegotiation(ParticipantContext participantContext, String negotiationId) {
        return store.findByIdAndLease(negotiationId)
                .flatMap(ServiceResult::from)
                .compose((cn) -> filterByParticipantContext(participantContext, cn));
    }

    private ServiceResult<ContractNegotiation> filterByParticipantContext(ParticipantContext participantContext, ContractNegotiation negotiation) {
        if (negotiation.getParticipantContextId().equals(participantContext.getParticipantContextId())) {
            return ServiceResult.success(negotiation);
        } else {
            return ServiceResult.notFound("No negotiation with id %s found".formatted(negotiation.getId()));
        }
    }

    private ServiceResult<ParticipantAgent> verifyRequest(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, Policy policy, RemoteMessage message) {
        return protocolTokenValidator.verify(participantContext, tokenRepresentation, RequestContractNegotiationPolicyContext::new, policy, message)
                .onFailure(failure -> monitor.debug(() -> "Verification Failed: %s".formatted(failure.getFailureDetail())));
    }

    private ServiceResult<ContractNegotiation> getNegotiation(ParticipantContext participantContext, String negotiationId) {
        return Optional.ofNullable(store.findById(negotiationId))
                .filter(cn -> participantContext.getParticipantContextId().equals(cn.getParticipantContextId()))
                .map(ServiceResult::success)
                .orElseGet(() -> ServiceResult.notFound("No negotiation with id %s found".formatted(negotiationId)));
    }

    private void update(ContractNegotiation negotiation) {
        store.save(negotiation);
        monitor.debug(() -> "[%s] ContractNegotiation %s is now in state %s."
                .formatted(negotiation.getType(), negotiation.getId(), negotiation.stateAsString()));
    }

    private void breakLease(ContractNegotiation process) {
        store.save(process);
    }

}
