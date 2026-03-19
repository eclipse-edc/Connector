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

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;

public abstract class AbstractContractNegotiationManager extends AbstractStateEntityManager<ContractNegotiation, ContractNegotiationStore> {

    protected ContractNegotiationPendingGuard pendingGuard = it -> false;
    protected NegotiationProcessors negotiationProcessors;

    abstract ContractNegotiation.Type type();

    protected Processor processNegotiationsInState(ContractNegotiationStates state, Function<ContractNegotiation, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{hasState(state.code()), isNotPending(), new Criterion("type", "=", type().name())};
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter), entityRetryProcessConfiguration, clock, monitor)
                .process(telemetry.contextPropagationMiddleware(function))
                .guard(pendingGuard, this::setPending)
                .onNotProcessed(this::breakLease)
                .build();
    }

    private CompletableFuture<StatusResult<Void>> setPending(ContractNegotiation contractNegotiation) {
        contractNegotiation.setPending(true);
        update(contractNegotiation);
        return CompletableFuture.completedFuture(StatusResult.success());
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
            Objects.requireNonNull(manager.negotiationProcessors, "negotiationProcessors");
            return manager;
        }

        public Builder<T> pendingGuard(ContractNegotiationPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }

        public Builder<T> negotiationProcessors(NegotiationProcessors negotiationProcessors) {
            manager.negotiationProcessors = negotiationProcessors;
            return this;
        }
    }

}
