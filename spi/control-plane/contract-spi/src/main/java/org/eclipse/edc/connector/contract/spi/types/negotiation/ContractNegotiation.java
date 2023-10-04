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
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.OFFERED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a contract negotiation.
 * <p>
 * Note: This class implements the negotiation process that is started by a consumer. For some use
 * cases, it may be interesting to initiate the contract negotiation as a provider.
 */
@JsonTypeName("dataspaceconnector:contractnegotiation")
@JsonDeserialize(builder = ContractNegotiation.Builder.class)
public class ContractNegotiation extends StatefulEntity<ContractNegotiation> {

    // constants used for JSON-LD transformation
    public static final String CONTRACT_NEGOTIATION_TYPE = EDC_NAMESPACE + "ContractNegotiation";
    public static final String CONTRACT_NEGOTIATION_AGREEMENT_ID = EDC_NAMESPACE + "contractAgreementId";
    public static final String CONTRACT_NEGOTIATION_COUNTERPARTY_ID = EDC_NAMESPACE + "counterPartyId";
    public static final String CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR = EDC_NAMESPACE + "counterPartyAddress";
    public static final String CONTRACT_NEGOTIATION_ERRORDETAIL = EDC_NAMESPACE + "errorDetail";
    public static final String CONTRACT_NEGOTIATION_PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String CONTRACT_NEGOTIATION_STATE = EDC_NAMESPACE + "state";
    public static final String CONTRACT_NEGOTIATION_NEG_TYPE = EDC_NAMESPACE + "type";
    public static final String CONTRACT_NEGOTIATION_CALLBACK_ADDR = EDC_NAMESPACE + "callbackAddresses";
    public static final String CONTRACT_NEGOTIATION_OFFERS = EDC_NAMESPACE + "offers";
    public static final String CONTRACT_NEGOTIATION_CREATED_AT = EDC_NAMESPACE + "createdAt";

    private List<CallbackAddress> callbackAddresses = new ArrayList<>();
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
     * Returns all callback addresses configured for the negotiation process.
     *
     * @return The callback addresses.
     */
    public List<CallbackAddress> getCallbackAddresses() {
        return Collections.unmodifiableList(callbackAddresses);
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
        transition(INITIAL, REQUESTING, INITIAL);
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
            transition(REQUESTED, OFFERED, INITIAL);
        } else {
            transition(REQUESTED, REQUESTED, REQUESTING);
        }
    }

    /**
     * Transition to state OFFERING (type provider only).
     */
    public void transitionOffering() {
        if (CONSUMER == type) {
            throw new IllegalStateException("Provider processes have no OFFERING state");
        }

        transition(OFFERING, OFFERING, OFFERED, REQUESTED);
    }

    /**
     * Transition to state OFFERED.
     */
    public void transitionOffered() {
        if (CONSUMER == type) {
            transition(OFFERED, OFFERED, REQUESTED);
        } else {
            transition(OFFERED, OFFERED, OFFERING);
        }
    }

    /**
     * Transition to state ACCEPTING (type consumer only).
     */
    public void transitionAccepting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no ACCEPTING state");
        }
        transition(ACCEPTING, ACCEPTING, REQUESTED);
    }

    /**
     * Transition to state ACCEPTED.
     */
    public void transitionAccepted() {
        transition(ACCEPTED, ACCEPTED, ACCEPTING, OFFERED);
    }

    /**
     * Transition to state AGREEING (type provider only).
     */
    public void transitionAgreeing() {
        if (CONSUMER == type) {
            throw new IllegalStateException("Consumer processes have no AGREEING state");
        }
        transition(AGREEING, AGREEING, REQUESTED, OFFERED, ACCEPTED);
    }

    /**
     * Transition to state AGREED.
     */
    public void transitionAgreed() {
        if (CONSUMER == type) {
            transition(AGREED, AGREEING, ACCEPTED, REQUESTED, AGREED);
        } else {
            transition(AGREED, AGREEING);
        }
    }

    /**
     * Transition to state VERIFYING.
     */
    public void transitionVerifying() {
        if (PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no VERIFYING state");
        }

        transition(VERIFYING, VERIFYING, AGREED, ACCEPTED);
    }

    /**
     * Transition to state VERIFIED.
     */
    public void transitionVerified() {
        if (type == CONSUMER) {
            transition(VERIFIED, VERIFIED, VERIFYING);
        } else {
            transition(VERIFIED, VERIFIED, ACCEPTED, AGREED);
        }
    }

    /**
     * Transition to state FINALIZING.
     */
    public void transitionFinalizing() {
        if (CONSUMER == type) {
            throw new IllegalStateException("Consumer processes have no FINALIZING state");
        }

        transition(FINALIZING, FINALIZING, VERIFIED);
    }

    /**
     * Transition to state FINALIZED.
     */
    public void transitionFinalized() {
        transition(FINALIZED, FINALIZED, FINALIZING, AGREED, VERIFIED);
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
        transition(TERMINATED, state -> canBeTerminated());
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
                .contractOffers(contractOffers)
                .callbackAddresses(callbackAddresses);
        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return ContractNegotiationStates.from(state).name();
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
        var that = (ContractNegotiation) o;
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

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            entity.callbackAddresses = callbackAddresses;
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
            if (entity.callbackAddresses == null) {
                entity.callbackAddresses = new ArrayList<>();
            }
            return entity;
        }
    }
}
