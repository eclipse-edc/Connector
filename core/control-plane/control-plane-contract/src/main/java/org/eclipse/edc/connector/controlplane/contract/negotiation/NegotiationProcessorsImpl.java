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

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.ACCEPTED;
import static org.eclipse.edc.statemachine.retry.processor.Process.futureResult;

public class NegotiationProcessorsImpl implements NegotiationProcessors {

    private final Monitor monitor;
    private final ProtocolWebhookResolver protocolWebhookResolver;
    private final ContractNegotiationObservable observable;
    private final ContractNegotiationStore store;
    private final EntityRetryProcessFactory entityRetryProcessFactory;
    private final ParticipantIdentityResolver identityResolver;
    private final Clock clock;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public NegotiationProcessorsImpl(Monitor monitor, ProtocolWebhookResolver protocolWebhookResolver,
                                     ContractNegotiationObservable observable, ContractNegotiationStore store,
                                     ParticipantIdentityResolver identityResolver, Clock clock,
                                     RemoteMessageDispatcherRegistry dispatcherRegistry,
                                     EntityRetryProcessConfiguration entityRetryProcessConfiguration) {
        this.monitor = monitor;
        this.protocolWebhookResolver = protocolWebhookResolver;
        this.observable = observable;
        this.store = store;
        this.identityResolver = identityResolver;
        this.clock = clock;
        this.dispatcherRegistry = dispatcherRegistry;
        this.entityRetryProcessFactory = new EntityRetryProcessFactory(monitor, clock, entityRetryProcessConfiguration);
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processInitial(ContractNegotiation negotiation) {
        transitionToRequesting(negotiation);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processRequesting(ContractNegotiation negotiation) {
        var callbackAddress = protocolWebhookResolver.getWebhook(negotiation.getParticipantContextId(), negotiation.getProtocol());
        if (callbackAddress == null) {
            var message = "No callback address found for protocol: %s".formatted(negotiation.getProtocol());
            transitionToTerminated(negotiation, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        var type = negotiation.getContractOffers().size() == 1 ? ContractRequestMessage.Type.INITIAL : ContractRequestMessage.Type.COUNTER_OFFER;

        var messageBuilder = ContractRequestMessage.Builder.newInstance()
                .contractOffer(negotiation.getLastContractOffer())
                .callbackAddress(callbackAddress.url())
                .type(type);

        return dispatch(messageBuilder, negotiation, ContractNegotiationAck.class, "[Consumer] send request")
                .onSuccess(this::transitionToRequested)
                .onFailure((n, throwable) -> transitionToRequesting(n))
                .onFinalFailure((n, throwable) -> transitionToTerminated(n, format("Failed to request contract to provider: %s", throwable.getMessage())))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processAccepting(ContractNegotiation negotiation) {
        var messageBuilder = ContractNegotiationEventMessage.Builder.newInstance().type(ACCEPTED);
        messageBuilder.policy(negotiation.getLastContractOffer().getPolicy());
        return dispatch(messageBuilder, negotiation, Object.class, "[consumer] send acceptance")
                .onSuccess((n, result) -> transitionToAccepted(n))
                .onFailure((n, throwable) -> transitionToAccepting(n))
                .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send acceptance to provider: %s", throwable.getMessage())))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processAgreed(ContractNegotiation negotiation) {
        transitionToVerifying(negotiation);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processVerifying(ContractNegotiation negotiation) {
        var messageBuilder = ContractAgreementVerificationMessage.Builder.newInstance()
                .policy(negotiation.getContractAgreement().getPolicy());

        return dispatch(messageBuilder, negotiation, Object.class, "[consumer] send verification")
                .onSuccess((n, result) -> transitionToVerified(n))
                .onFailure((n, throwable) -> transitionToVerifying(n))
                .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send verification to provider: %s", throwable.getMessage())))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processOffering(ContractNegotiation negotiation) {
        var callbackAddress = protocolWebhookResolver.getWebhook(negotiation.getParticipantContextId(), negotiation.getProtocol());
        if (callbackAddress == null) {
            var message = "No callback address found for protocol: %s".formatted(negotiation.getProtocol());
            transitionToTerminated(negotiation, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        var messageBuilder = ContractOfferMessage.Builder.newInstance()
                .contractOffer(negotiation.getLastContractOffer())
                .callbackAddress(callbackAddress.url());

        return dispatch(messageBuilder, negotiation, ContractNegotiationAck.class, "[Provider] send offer")
                .onSuccess(this::transitionToOffered)
                .onFailure((n, throwable) -> transitionToOffering(n))
                .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send offer to consumer: %s", throwable.getMessage())))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processRequested(ContractNegotiation negotiation) {
        transitionToAgreeing(negotiation);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processAccepted(ContractNegotiation negotiation) {
        transitionToAgreeing(negotiation);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processAgreeing(ContractNegotiation negotiation) {
        var callbackAddress = protocolWebhookResolver.getWebhook(negotiation.getParticipantContextId(), negotiation.getProtocol());
        if (callbackAddress == null) {
            var message = "No callback address found for protocol: %s".formatted(negotiation.getProtocol());
            transitionToTerminated(negotiation, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        var agreement = Optional.ofNullable(negotiation.getContractAgreement())
                .orElseGet(() -> {
                    var lastOffer = negotiation.getLastContractOffer();
                    var protocol = negotiation.getProtocol();
                    var providerId = identityResolver.getParticipantId(negotiation.getParticipantContextId(), protocol);

                    var contractPolicy = lastOffer.getPolicy().toBuilder().type(PolicyType.CONTRACT)
                            .assignee(negotiation.getCounterPartyId())
                            .assigner(providerId)
                            .build();

                    return ContractAgreement.Builder.newInstance()
                            .contractSigningDate(clock.instant().getEpochSecond())
                            .providerId(providerId)
                            .consumerId(negotiation.getCounterPartyId())
                            .policy(contractPolicy)
                            .assetId(lastOffer.getAssetId())
                            .participantContextId(negotiation.getParticipantContextId())
                            .build();
                });

        var messageBuilder = ContractAgreementMessage.Builder.newInstance()
                .callbackAddress(callbackAddress.url())
                .contractAgreement(agreement);

        return dispatch(messageBuilder, negotiation, Object.class, "[Provider] send agreement")
                .onSuccess((n, result) -> transitionToAgreed(n, agreement))
                .onFailure((n, throwable) -> transitionToAgreeing(n))
                .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send agreement to consumer: %s", throwable.getMessage())))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processVerified(ContractNegotiation negotiation) {
        transitionToFinalizing(negotiation);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processFinalizing(ContractNegotiation negotiation) {
        var messageBuilder = ContractNegotiationEventMessage.Builder.newInstance()
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .policy(negotiation.getContractAgreement().getPolicy());

        return dispatch(messageBuilder, negotiation, Object.class, "[Provider] send finalization")
                .onSuccess((n, result) -> transitionToFinalized(n))
                .onFailure((n, throwable) -> transitionToFinalizing(n))
                .onFinalFailure((n, throwable) -> transitionToTerminating(n, format("Failed to send finalization to consumer: %s", throwable.getMessage())))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processTerminating(ContractNegotiation negotiation) {
        var messageBuilder = ContractNegotiationTerminationMessage.Builder.newInstance()
                .rejectionReason(negotiation.getErrorDetail())
                .policy(negotiation.getLastContractOffer().getPolicy());

        return dispatch(messageBuilder, negotiation, Object.class, "[%s] send termination".formatted(negotiation.getType().name()))
                .onSuccess((n, result) -> transitionToTerminated(n))
                .onFailure((n, throwable) -> transitionToTerminating(n))
                .onFinalFailure((n, throwable) -> transitionToTerminated(n, format("Failed to send termination to counter party: %s", throwable.getMessage())))
                .execute();
    }


    private <T> RetryProcessor<ContractNegotiation, T> dispatch(ProcessRemoteMessage.Builder<?, ?> messageBuilder,
                                                                ContractNegotiation negotiation, Class<T> responseType, String name) {
        messageBuilder.counterPartyAddress(negotiation.getCounterPartyAddress())
                .counterPartyId(negotiation.getCounterPartyId())
                .protocol(negotiation.getProtocol())
                .processId(Optional.ofNullable(negotiation.getCorrelationId()).orElse(negotiation.getId()));

        if (negotiation.getType() == ContractNegotiation.Type.CONSUMER) {
            messageBuilder.consumerPid(negotiation.getId()).providerPid(negotiation.getCorrelationId());
        } else {
            messageBuilder.providerPid(negotiation.getId()).consumerPid(negotiation.getCorrelationId());
        }

        if (negotiation.lastSentProtocolMessage() != null) {
            messageBuilder.id(negotiation.lastSentProtocolMessage());
        }

        var message = messageBuilder.build();

        negotiation.lastSentProtocolMessage(message.getId());

        return entityRetryProcessFactory.retryProcessor(negotiation)
                .doProcess(futureResult(name, (n, v) -> dispatcherRegistry.dispatch(negotiation.getParticipantContextId(), responseType, message)));
    }

    private void transitionToRequesting(ContractNegotiation negotiation) {
        negotiation.transitionRequesting();
        update(negotiation);
    }

    private void transitionToRequested(ContractNegotiation negotiation, ContractNegotiationAck ack) {
        negotiation.transitionRequested();
        negotiation.setCorrelationId(ack.getProviderPid());
        update(negotiation);
        observable.invokeForEach(l -> l.requested(negotiation));
    }

    private void transitionToAccepting(ContractNegotiation negotiation) {
        negotiation.transitionAccepting();
        update(negotiation);
    }

    private void transitionToAccepted(ContractNegotiation negotiation) {
        negotiation.transitionAccepted();
        update(negotiation);
        observable.invokeForEach(l -> l.accepted(negotiation));
    }

    private void transitionToOffering(ContractNegotiation negotiation) {
        negotiation.transitionOffering();
        update(negotiation);
    }

    private void transitionToOffered(ContractNegotiation negotiation, ContractNegotiationAck ack) {
        negotiation.transitionOffered();
        negotiation.setCorrelationId(ack.getConsumerPid());
        update(negotiation);
        observable.invokeForEach(l -> l.offered(negotiation));
    }

    private void transitionToAgreeing(ContractNegotiation negotiation) {
        negotiation.transitionAgreeing();
        update(negotiation);
    }

    private void transitionToAgreed(ContractNegotiation negotiation, ContractAgreement agreement) {
        negotiation.setContractAgreement(agreement);
        negotiation.transitionAgreed();
        update(negotiation);
        observable.invokeForEach(l -> l.agreed(negotiation));
    }

    private void transitionToVerifying(ContractNegotiation negotiation) {
        negotiation.transitionVerifying();
        update(negotiation);
    }

    private void transitionToVerified(ContractNegotiation negotiation) {
        negotiation.transitionVerified();
        update(negotiation);
        observable.invokeForEach(l -> l.verified(negotiation));
    }

    private void transitionToFinalizing(ContractNegotiation negotiation) {
        negotiation.transitionFinalizing();
        update(negotiation);
    }

    private void transitionToFinalized(ContractNegotiation negotiation) {
        negotiation.transitionFinalized();
        update(negotiation);
        observable.invokeForEach(l -> l.finalized(negotiation));
    }

    private void transitionToTerminating(ContractNegotiation negotiation, String message) {
        negotiation.transitionTerminating(message);
        update(negotiation);
    }

    private void transitionToTerminating(ContractNegotiation negotiation) {
        negotiation.transitionTerminating();
        update(negotiation);
    }

    private void transitionToTerminated(ContractNegotiation negotiation, String message) {
        negotiation.setErrorDetail(message);
        transitionToTerminated(negotiation);
    }

    private void transitionToTerminated(ContractNegotiation negotiation) {
        negotiation.transitionTerminated();
        update(negotiation);
        observable.invokeForEach(l -> l.terminated(negotiation));
    }

    protected StoreResult<Void> update(ContractNegotiation entity) {
        return store.save(entity)
                .onSuccess(ignored -> {
                    var error = entity.getErrorDetail() == null ? "" : ". errorDetail: " + entity.getErrorDetail();

                    monitor.debug(() -> "[%s] %s %s is now in state %s%s"
                            .formatted(this.getClass().getSimpleName(), entity.getClass().getSimpleName(),
                                    entity.getId(), entity.stateAsString(), error));
                });
    }
}
