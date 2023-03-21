/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - extended method implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_AGREEING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_REQUESTING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_VERIFIED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_VERIFYING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_AGREEING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_FINALIZED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_FINALIZING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_OFFERED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;

/**
 * Represents a contract negotiation.
 * <p>
 * Note: This class implements the negotiation process that is started by a consumer. For some use
 * cases, it may be interesting to initiate the contract negotiation as a provider.
 *
 * <p>
 * TODO: This is only placeholder
 * TODO: Implement state transitions
 * TODO: Add error details
 */
@JsonTypeName("dataspaceconnector:contractnegotiation")
@JsonDeserialize(builder = ContractNegotiation.Builder.class)
public class ContractNegotiation extends StatefulEntity<ContractNegotiation> {
    private String correlationId;
    private String counterPartyId;
    private String counterPartyAddress;
    private String protocol;
    private Type type = CONSUMER;
    private ContractAgreement contractAgreement;
    private List<ContractOffer> contractOffers = new ArrayList<>();

    public Type getType() {
        return type;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    /**
     * Returns the correlation id sent by the client or null if this is a client-side negotiation.
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns the data protocol used for this negotiation.
     *
     * @return The protocol.
     */
    @NotNull
    public String getProtocol() {
        return protocol;
    }

    /**
     * Returns all contract offers which have been part of the negotiation process.
     *
     * @return The contract offers.
     */
    public List<ContractOffer> getContractOffers() {
        return contractOffers;
    }

    /**
     * Adds a new contract offer to this negotiation.
     *
     * @param offer The offer to add.
     */
    public void addContractOffer(ContractOffer offer) {
        contractOffers.add(offer);
    }

    /**
     * Returns the last offer in the list of contract offers.
     */
    public ContractOffer getLastContractOffer() {
        var size = contractOffers.size();
        if (size == 0) {
            return null;
        }
        return contractOffers.get(size - 1);
    }

    /**
     * Returns the finalized agreement or null if the negotiation has not been confirmed.
     */
    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    /**
     * Sets the agreement for this negotiation.
     *
     * @param agreement the agreement.
     */
    public void setContractAgreement(ContractAgreement agreement) {
        contractAgreement = agreement;
        setModified();
    }

    /**
     * Transition to state INITIAL.
     */
    public void transitionInitial() {
        transition(INITIAL, CONSUMER_REQUESTING, INITIAL);
    }

    /**
     * Transition to state CONSUMER_REQUESTING (type consumer only).
     */
    public void transitionConsumerRequesting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no CONSUMER_REQUESTING state");
        }
        transition(CONSUMER_REQUESTING, CONSUMER_REQUESTING, INITIAL);
    }

    /**
     * Transition to state CONSUMER_REQUESTED.
     */
    public void transitionConsumerRequested() {
        if (Type.PROVIDER == type) {
            transition(CONSUMER_REQUESTED, INITIAL);
        } else {
            transition(CONSUMER_REQUESTED, CONSUMER_REQUESTED, CONSUMER_REQUESTING);
        }
    }

    /**
     * Transition to state PROVIDER_OFFERING (type provider only).
     */
    public void transitionProviderOffering() {
        if (CONSUMER == type) {
            throw new IllegalStateException("Provider processes have no PROVIDER_OFFERING state");
        }

        transition(PROVIDER_OFFERING, PROVIDER_OFFERING, PROVIDER_OFFERED, CONSUMER_REQUESTED);
    }

    /**
     * Transition to state PROVIDER_OFFERED.
     */
    public void transitionProviderOffered() {
        if (CONSUMER == type) {
            transition(PROVIDER_OFFERED, PROVIDER_OFFERED, CONSUMER_REQUESTED);
        } else {
            transition(PROVIDER_OFFERED, PROVIDER_OFFERED, PROVIDER_OFFERING);
        }
    }

    /**
     * Transition to state CONSUMER_APPROVING (type consumer only).
     */
    public void transitionConsumerAgreeing() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no CONSUMER_APPROVING state");
        }
        transition(CONSUMER_AGREEING, CONSUMER_AGREEING, CONSUMER_REQUESTED);
    }

    /**
     * Transition to state CONSUMER_AGREED.
     */
    public void transitionConsumerAgreed() {
        transition(CONSUMER_AGREED, CONSUMER_AGREED, CONSUMER_AGREEING, PROVIDER_OFFERED);
    }

    /**
     * Transition to state PROVIDER_AGREEING (type provider only).
     */
    public void transitionProviderAgreeing() {
        if (CONSUMER == type) {
            throw new IllegalStateException("Consumer processes have no PROVIDER_AGREEING state");
        }
        transition(PROVIDER_AGREEING, PROVIDER_AGREEING, CONSUMER_REQUESTED, PROVIDER_OFFERED);
    }

    /**
     * Transition to state PROVIDER_AGREED.
     */
    public void transitionProviderAgreed() {
        if (CONSUMER == type) {
            transition(PROVIDER_AGREED, PROVIDER_AGREEING, CONSUMER_AGREED, CONSUMER_REQUESTED, PROVIDER_AGREED);
        } else {
            transition(PROVIDER_AGREED, PROVIDER_AGREEING);
        }
    }

    /**
     * Transition to state CONSUMER_VERIFYING.
     */
    public void transitionConsumerVerifying() {
        if (PROVIDER == type) {
            throw new IllegalStateException("Consumer processes have no CONSUMER_VERIFYING state");
        }

        transition(CONSUMER_VERIFYING, CONSUMER_VERIFYING, PROVIDER_AGREED, CONSUMER_AGREED);
    }

    /**
     * Transition to state CONSUMER_VERIFIED.
     */
    public void transitionConsumerVerified() {
        if (type == CONSUMER) {
            transition(CONSUMER_VERIFIED, CONSUMER_VERIFIED, CONSUMER_VERIFYING);
        } else {
            transition(CONSUMER_VERIFIED, CONSUMER_VERIFIED, CONSUMER_AGREED, PROVIDER_AGREED);
        }
    }

    /**
     * Transition to state PROVIDER_FINALIZING.
     */
    public void transitionProviderFinalizing() {
        if (CONSUMER == type) {
            throw new IllegalStateException("Consumer processes have no PROVIDER_FINALIZING state");
        }

        transition(PROVIDER_FINALIZING, PROVIDER_FINALIZING, CONSUMER_VERIFIED);
    }

    /**
     * Transition to state PROVIDER_FINALIZED.
     */
    public void transitionProviderFinalized() {
        transition(PROVIDER_FINALIZED, PROVIDER_FINALIZED, PROVIDER_FINALIZING, PROVIDER_AGREED, CONSUMER_VERIFIED);
    }

    /**
     * Tells if the negotiation can be terminated, so if it can be put in the `TERMINATING` state
     *
     * @return true if the negotiation can be terminated, false otherwise
     */
    public boolean canBeTerminated() {
        return true;
    }

    /**
     * Transition to state TERMINATING.
     *
     * @param errorDetail Message describing the error.
     */
    public void transitionTerminating(@Nullable String errorDetail) {
        this.errorDetail = errorDetail;
        transitionTerminating();
    }

    /**
     * Transition to state TERMINATING.
     */
    public void transitionTerminating() {
        transition(TERMINATING, state -> canBeTerminated());
    }

    /**
     * Transition to state TERMINATED.
     */
    public void transitionTerminated() {
        if (CONSUMER == type) {
            transition(TERMINATED, TERMINATING, CONSUMER_REQUESTED);
        } else {
            transition(TERMINATED, TERMINATING, PROVIDER_OFFERED, PROVIDER_AGREEING, PROVIDER_AGREED, CONSUMER_REQUESTED);
        }
    }

    /**
     * Create a copy of this negotiation.
     *
     * @return The copy.
     */
    @Override
    public ContractNegotiation copy() {
        var builder = Builder.newInstance()
                .correlationId(correlationId)
                .counterPartyId(counterPartyId)
                .counterPartyAddress(counterPartyAddress)
                .protocol(protocol)
                .type(type)
                .contractAgreement(contractAgreement)
                .contractOffers(contractOffers);
        return copy(builder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, correlationId, counterPartyId, clock, protocol, traceContext, type, state, stateCount, stateTimestamp, contractAgreement, contractOffers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContractNegotiation that = (ContractNegotiation) o;
        return state == that.state && stateCount == that.stateCount && stateTimestamp == that.stateTimestamp && Objects.equals(id, that.id) &&
                Objects.equals(correlationId, that.correlationId) && Objects.equals(counterPartyId, that.counterPartyId) &&
                Objects.equals(clock, that.clock) &&
                Objects.equals(protocol, that.protocol) && Objects.equals(traceContext, that.traceContext) &&
                type == that.type && Objects.equals(contractAgreement, that.contractAgreement) && Objects.equals(contractOffers, that.contractOffers);
    }

    /**
     * Transition to a given end state from an allowed number of previous states. Increases the
     * state count if transitioned to the same state and updates the state timestamp.
     *
     * @param end    The desired state.
     * @param starts The allowed previous states.
     */
    private void transition(ContractNegotiationStates end, ContractNegotiationStates... starts) {
        transition(end, (state) -> Arrays.stream(starts).anyMatch(s -> s == state));
    }

    /**
     * Transition to a given end state if the passed predicate test correctly. Increases the
     * state count if transitioned to the same state and updates the state timestamp.
     *
     * @param end          The desired state.
     * @param canTransitTo Tells if the negotiation can transit to that state.
     */
    private void transition(ContractNegotiationStates end, Predicate<ContractNegotiationStates> canTransitTo) {
        if (!canTransitTo.test(ContractNegotiationStates.from(state))) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", ContractNegotiationStates.from(state), ContractNegotiationStates.from(end.code())));
        }
        transitionTo(end.code());
    }

    public enum Type {
        CONSUMER, PROVIDER;
    }

    /**
     * Builder for ContractNegotiation.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends StatefulEntity.Builder<ContractNegotiation, Builder> {


        private Builder(ContractNegotiation negotiation) {
            super(negotiation);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new ContractNegotiation());
        }

        public Builder protocol(String protocol) {
            entity.protocol = protocol;
            return this;
        }

        public Builder counterPartyId(String id) {
            entity.counterPartyId = id;
            return this;
        }

        public Builder counterPartyAddress(String address) {
            entity.counterPartyAddress = address;
            return this;
        }

        public Builder correlationId(String id) {
            entity.correlationId = id;
            return this;
        }

        public Builder contractAgreement(ContractAgreement agreement) {
            entity.contractAgreement = agreement;
            return this;
        }

        //used mainly for JSON deserialization
        public Builder contractOffers(List<ContractOffer> contractOffers) {
            entity.contractOffers = contractOffers;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            entity.contractOffers.add(contractOffer);
            return this;
        }

        public Builder type(Type type) {
            entity.type = type;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ContractNegotiation build() {
            super.build();

            Objects.requireNonNull(entity.counterPartyId);
            Objects.requireNonNull(entity.counterPartyAddress);
            Objects.requireNonNull(entity.protocol);
            if (Type.PROVIDER == entity.type) {
                Objects.requireNonNull(entity.correlationId);
            }
            if (entity.state == 0) {
                entity.transitionTo(INITIAL.code());
            }
            return entity;
        }
    }
}
