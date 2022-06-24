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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_APPROVED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_APPROVING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_OFFERED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_OFFERING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.PROVIDER_OFFERED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.UNSAVED;

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
    private Type type = Type.CONSUMER;
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
    }

    /**
     * Transition to state INITIAL.
     */
    public void transitionInitial() {
        transition(INITIAL, REQUESTING, UNSAVED);
    }

    /**
     * Transition to state REQUESTING (type consumer only).
     */
    public void transitionRequesting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTING state");
        }
        transition(REQUESTING, REQUESTING, INITIAL);
    }

    /**
     * Transition to state REQUESTED.
     */
    public void transitionRequested() {
        if (Type.PROVIDER == type) {
            transition(REQUESTED, UNSAVED);
        } else {
            transition(REQUESTED, REQUESTED, REQUESTING);
        }
    }

    /**
     * Transition to state REQUESTED.
     */
    public void transitionOffering() {
        if (Type.CONSUMER == type) {
            transition(CONSUMER_OFFERING, CONSUMER_OFFERING, REQUESTED);
        } else {
            transition(PROVIDER_OFFERING, PROVIDER_OFFERING, PROVIDER_OFFERED, REQUESTED);
        }
    }

    /**
     * Transition to state CONSUMER_OFFERED for type consumer and PROVIDER_OFFERED for type
     * provider.
     */
    public void transitionOffered() {
        if (Type.CONSUMER == type) {
            transition(CONSUMER_OFFERED, PROVIDER_OFFERED, CONSUMER_OFFERING);
        } else {
            transition(PROVIDER_OFFERED, PROVIDER_OFFERED, PROVIDER_OFFERING);
        }
    }

    /**
     * Transition to state CONSUMER_APPROVING (type consumer only).
     */
    public void transitionApproving() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no CONSUMER_APPROVING state");
        }
        transition(CONSUMER_APPROVING, CONSUMER_APPROVING, CONSUMER_OFFERED, REQUESTED);
    }

    /**
     * Transition to state CONSUMER_APPROVED (type consumer only).
     */
    public void transitionApproved() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no CONSUMER_APPROVED state");
        }
        transition(CONSUMER_APPROVED, CONSUMER_APPROVED, CONSUMER_APPROVING, PROVIDER_OFFERED);
    }

    /**
     * Transition to state DECLINING.
     */
    public void transitionDeclining() {
        if (Type.CONSUMER == type) {
            transition(DECLINING, DECLINING, REQUESTED, CONSUMER_OFFERED, CONSUMER_APPROVED);
        } else {
            transition(DECLINING, DECLINING, REQUESTED, PROVIDER_OFFERED, CONSUMER_APPROVED);
        }
    }

    /**
     * Transition to state DECLINED.
     */
    public void transitionDeclined() {
        if (Type.CONSUMER == type) {
            transition(DECLINED, DECLINING, CONSUMER_OFFERED, REQUESTED);
        } else {
            transition(DECLINED, DECLINING, PROVIDER_OFFERED, CONFIRMING, CONFIRMED, REQUESTED);
        }

    }

    /**
     * Transition to state CONFIRMING (type provider only).
     */
    public void transitionConfirming() {
        if (Type.CONSUMER == type) {
            throw new IllegalStateException("Consumer processes have no CONFIRMING state");
        }
        transition(CONFIRMING, CONFIRMING, REQUESTED, PROVIDER_OFFERED);
    }

    /**
     * Transition to state CONFIRMED.
     */
    public void transitionConfirmed() {
        if (Type.CONSUMER == type) {
            transition(CONFIRMED, CONFIRMING, CONSUMER_APPROVED, REQUESTED, CONSUMER_OFFERED, CONFIRMED);
        } else {
            transition(CONFIRMED, CONFIRMING);
        }

    }

    /**
     * Transition to state ERROR.
     *
     * @param errorDetail Message describing the error.
     */
    public void transitionError(@Nullable String errorDetail) {
        state = ContractNegotiationStates.ERROR.code();
        this.errorDetail = errorDetail;
        stateCount = 1;
        updateStateTimestamp();
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

    @Override
    public int hashCode() {
        return Objects.hash(id, correlationId, counterPartyId, clock, protocol, traceContext, type, state, stateCount, stateTimestamp, contractAgreement, contractOffers);
    }

    /**
     * Transition to a given end state from an allowed number of previous states. Increases the
     * state count if transitioned to the same state and updates the state timestamp.
     *
     * @param end    The desired state.
     * @param starts The allowed previous states.
     */
    private void transition(ContractNegotiationStates end, ContractNegotiationStates... starts) {
        if (Arrays.stream(starts).noneMatch(s -> s.code() == state)) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", ContractNegotiationStates.from(state), ContractNegotiationStates.from(end.code())));
        }
        transitionTo(end.code());
    }

    public enum Type {
        CONSUMER, PROVIDER
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
            Objects.requireNonNull(entity.counterPartyId);
            Objects.requireNonNull(entity.counterPartyAddress);
            Objects.requireNonNull(entity.protocol);
            if (Type.PROVIDER == entity.type) {
                Objects.requireNonNull(entity.correlationId);
            }
            return super.build();
        }
    }
}
