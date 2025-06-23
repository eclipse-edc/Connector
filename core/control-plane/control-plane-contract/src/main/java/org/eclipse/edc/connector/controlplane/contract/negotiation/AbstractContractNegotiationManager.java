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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.retry.processor.Process;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;

public abstract class AbstractContractNegotiationManager extends AbstractStateEntityManager<ContractNegotiation, ContractNegotiationStore> {

    protected String participantId;
    protected RemoteMessageDispatcherRegistry dispatcherRegistry;
    protected ContractNegotiationObservable observable;
    protected PolicyDefinitionStore policyStore;
    protected DataspaceProfileContextRegistry dataspaceProfileContextRegistry;
    protected ContractNegotiationPendingGuard pendingGuard = it -> false;

    abstract ContractNegotiation.Type type();

    protected Processor processNegotiationsInState(ContractNegotiationStates state, Function<ContractNegotiation, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending(), new Criterion("type", "=", type().name()) };
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .guard(pendingGuard, this::setPending)
                .onNotProcessed(this::breakLease)
                .build();
    }

    /**
     * Processes {@link ContractNegotiation} in state TERMINATING. Tries to send a contract termination to the counter-party.
     * If this succeeds, the ContractNegotiation is transitioned to state TERMINATED. Else, it is transitioned
     * to TERMINATING for a retry.
     *
     * @return true if processed, false elsewhere
     */
    @WithSpan
    protected boolean processTerminating(ContractNegotiation negotiation) {
        var messageBuilder = ContractNegotiationTerminationMessage.Builder.newInstance()
                .rejectionReason(negotiation.getErrorDetail())
                .policy(negotiation.getLastContractOffer().getPolicy());

        return dispatch(messageBuilder, negotiation, Object.class, "[%s] send termination".formatted(type().name()))
                .onSuccess((n, result) -> transitionToTerminated(n))
                .onFailure((n, throwable) -> transitionToTerminating(n))
                .onFinalFailure((n, throwable) -> transitionToTerminated(n, format("Failed to send termination to counter party: %s", throwable.getMessage())))
                .execute();
    }

    protected <T> RetryProcessor<ContractNegotiation, T> dispatch(ProcessRemoteMessage.Builder<?, ?> messageBuilder,
                                                                  ContractNegotiation negotiation, Class<T> responseType, String name) {
        messageBuilder.counterPartyAddress(negotiation.getCounterPartyAddress())
                .counterPartyId(negotiation.getCounterPartyId())
                .protocol(negotiation.getProtocol())
                .processId(Optional.ofNullable(negotiation.getCorrelationId()).orElse(negotiation.getId()));

        if (type() == ContractNegotiation.Type.CONSUMER) {
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
                .doProcess(Process.futureResult(name, (n, v) -> dispatcherRegistry.dispatch(responseType, message)));
    }

    protected void transitionToInitial(ContractNegotiation negotiation) {
        negotiation.transitionInitial();
        update(negotiation);
        observable.invokeForEach(l -> l.initiated(negotiation));
    }

    protected void transitionToRequesting(ContractNegotiation negotiation) {
        negotiation.transitionRequesting();
        update(negotiation);
    }

    protected void transitionToRequested(ContractNegotiation negotiation, ContractNegotiationAck ack) {
        negotiation.transitionRequested();
        negotiation.setCorrelationId(ack.getProviderPid());
        update(negotiation);
        observable.invokeForEach(l -> l.requested(negotiation));
    }

    protected void transitionToAccepting(ContractNegotiation negotiation) {
        negotiation.transitionAccepting();
        update(negotiation);
    }

    protected void transitionToAccepted(ContractNegotiation negotiation) {
        negotiation.transitionAccepted();
        update(negotiation);
        observable.invokeForEach(l -> l.accepted(negotiation));
    }

    protected void transitionToOffering(ContractNegotiation negotiation) {
        negotiation.transitionOffering();
        update(negotiation);
    }

    protected void transitionToOffered(ContractNegotiation negotiation, ContractNegotiationAck ack) {
        negotiation.transitionOffered();
        negotiation.setCorrelationId(ack.getConsumerPid());
        update(negotiation);
        observable.invokeForEach(l -> l.offered(negotiation));
    }

    protected void transitionToAgreeing(ContractNegotiation negotiation) {
        negotiation.transitionAgreeing();
        update(negotiation);
    }

    protected void transitionToAgreed(ContractNegotiation negotiation, ContractAgreement agreement) {
        negotiation.setContractAgreement(agreement);
        negotiation.transitionAgreed();
        update(negotiation);
        observable.invokeForEach(l -> l.agreed(negotiation));
    }

    protected void transitionToVerifying(ContractNegotiation negotiation) {
        negotiation.transitionVerifying();
        update(negotiation);
    }

    protected void transitionToVerified(ContractNegotiation negotiation) {
        negotiation.transitionVerified();
        update(negotiation);
        observable.invokeForEach(l -> l.verified(negotiation));
    }

    protected void transitionToFinalizing(ContractNegotiation negotiation) {
        negotiation.transitionFinalizing();
        update(negotiation);
    }

    protected void transitionToFinalized(ContractNegotiation negotiation) {
        negotiation.transitionFinalized();
        update(negotiation);
        observable.invokeForEach(l -> l.finalized(negotiation));
    }

    protected void transitionToTerminating(ContractNegotiation negotiation, String message) {
        negotiation.transitionTerminating(message);
        update(negotiation);
    }

    protected void transitionToTerminating(ContractNegotiation negotiation) {
        negotiation.transitionTerminating();
        update(negotiation);
    }

    protected void transitionToTerminated(ContractNegotiation negotiation, String message) {
        negotiation.setErrorDetail(message);
        transitionToTerminated(negotiation);
    }

    protected void transitionToTerminated(ContractNegotiation negotiation) {
        negotiation.transitionTerminated();
        update(negotiation);
        observable.invokeForEach(l -> l.terminated(negotiation));
    }

    private boolean setPending(ContractNegotiation contractNegotiation) {
        contractNegotiation.setPending(true);
        update(contractNegotiation);
        return true;
    }

    public static class Builder<T extends AbstractContractNegotiationManager>
            extends AbstractStateEntityManager.Builder<ContractNegotiation, ContractNegotiationStore, T, Builder<T>> {

        protected Builder(T manager) {
            super(manager);
        }

        @Override
        public Builder<T> self() {
            return this;
        }

        @Override
        public T build() {
            super.build();
            Objects.requireNonNull(manager.participantId, "participantId");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.observable, "observable");

            Objects.requireNonNull(manager.policyStore, "policyStore");
            return manager;
        }

        public Builder<T> participantId(String id) {
            manager.participantId = id;
            return this;
        }

        public Builder<T> dispatcherRegistry(RemoteMessageDispatcherRegistry dispatcherRegistry) {
            manager.dispatcherRegistry = dispatcherRegistry;
            return this;
        }

        public Builder<T> observable(ContractNegotiationObservable observable) {
            manager.observable = observable;
            return this;
        }

        public Builder<T> policyStore(PolicyDefinitionStore policyStore) {
            manager.policyStore = policyStore;
            return this;
        }

        public Builder<T> dataspaceProfileContextRegistry(DataspaceProfileContextRegistry dataspaceProfileContextRegistry) {
            manager.dataspaceProfileContextRegistry = dataspaceProfileContextRegistry;
            return this;
        }

        public Builder<T> pendingGuard(ContractNegotiationPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }
    }

}
