/*
 *  Copyright (c) 2021 Microsoft Corporation
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
 *
 */
package org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Represents a contract negotiation.
 *
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
public class ContractNegotiation {
    public enum Type {
        CONSUMER, PROVIDER
    }

    private String id;
    private String correlationId;
    private String counterPartyId;
    private String counterPartyAddress;
    private String protocol;

    private Type type = Type.CONSUMER;

    private int state;
    private int stateCount;
    private long stateTimestamp;
    private String errorDetail;

    private ContractAgreement contractAgreement;
    private List<ContractOffer> contractOffers = new ArrayList<>();

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
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
     */
    @NotNull
    public String getProtocol() {
        return protocol;
    }

    /**
     * Returns the current negotiation state.
     */
    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public List<ContractOffer> getContractOffers() {
        return contractOffers;
    }

    public void addContractOffer(ContractOffer offer) {
        contractOffers.add(offer);
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    /**
     * Returns the last offer in the list of contract offers.
     */
    public ContractOffer getLastContractOffer() {
        var size = contractOffers.size();
        return contractOffers.get(size - 1);
    }

    /**
     * Returns the finalized agreement or null if the negotiation has not been confirmed.
     */
    public ContractAgreement getContractAgreement() {
        return contractAgreement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, correlationId, counterPartyId, protocol, type, state, stateCount, stateTimestamp, contractAgreement, contractOffers);
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
                Objects.equals(correlationId, that.correlationId) && Objects.equals(counterPartyId, that.counterPartyId) && Objects.equals(protocol, that.protocol) &&
                type == that.type && Objects.equals(contractAgreement, that.contractAgreement) && Objects.equals(contractOffers, that.contractOffers);
    }

    /**
     * Sets the agreement for this negotiation.
     *
     * @param agreement the agreement.
     */
    public void setContractAgreement(ContractAgreement agreement) {
        this.contractAgreement = agreement;
    }

    /**
     * Change state from unsaved to requesting.
     */
    public void transitionRequesting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTING state");
        }
        transition(ContractNegotiationStates.REQUESTING, ContractNegotiationStates.UNSAVED);
    }

    /**
     * Change state from requesting to requested.
     */
    public void transitionRequested() {
        if (Type.PROVIDER == type) {
            transition(ContractNegotiationStates.REQUESTED, ContractNegotiationStates.UNSAVED);
        } else {
            transition(ContractNegotiationStates.REQUESTED, ContractNegotiationStates.REQUESTING);
        }
    }

    public void transitionOffering() {
        if (Type.CONSUMER == type) {
            transition(ContractNegotiationStates.CONSUMER_OFFERING, ContractNegotiationStates.REQUESTED);
        } else {
            transition(ContractNegotiationStates.PROVIDER_OFFERING, ContractNegotiationStates.PROVIDER_OFFERING, ContractNegotiationStates.PROVIDER_OFFERED, ContractNegotiationStates.REQUESTED);
        }
    }

    public void transitionOffered() {
        if (Type.CONSUMER == type) {
            transition(ContractNegotiationStates.CONSUMER_OFFERED, ContractNegotiationStates.CONSUMER_OFFERING);
        } else {
            transition(ContractNegotiationStates.PROVIDER_OFFERED, ContractNegotiationStates.PROVIDER_OFFERING);
        }
    }

    public void transitionApproving() {
        transition(ContractNegotiationStates.CONSUMER_APPROVING, ContractNegotiationStates.CONSUMER_OFFERED, ContractNegotiationStates.REQUESTED);
    }

    public void transitionApproved() {
        transition(ContractNegotiationStates.CONSUMER_APPROVED, ContractNegotiationStates.CONSUMER_APPROVING, ContractNegotiationStates.PROVIDER_OFFERED);
    }

    /**
     * Change state to declining.
     */
    public void transitionDeclining() {
        transition(ContractNegotiationStates.DECLINING, ContractNegotiationStates.DECLINING, ContractNegotiationStates.REQUESTED, ContractNegotiationStates.PROVIDER_OFFERED, ContractNegotiationStates.CONSUMER_OFFERED, ContractNegotiationStates.CONSUMER_APPROVED);
    }

    /**
     * Change state to declined.
     */
    public void transitionDeclined() {
        transition(ContractNegotiationStates.DECLINED, ContractNegotiationStates.DECLINING, ContractNegotiationStates.PROVIDER_OFFERED, ContractNegotiationStates.CONSUMER_OFFERED, ContractNegotiationStates.CONFIRMED);
    }

    /**
     * Change state to confirming.
     */
    public void transitionConfirming() {
        transition(ContractNegotiationStates.CONFIRMING, ContractNegotiationStates.CONFIRMING, ContractNegotiationStates.REQUESTED, ContractNegotiationStates.PROVIDER_OFFERED);
    }

    /**
     * Change state to confirmed.
     */
    public void transitionConfirmed() {
        transition(ContractNegotiationStates.CONFIRMED, ContractNegotiationStates.CONFIRMING, ContractNegotiationStates.CONSUMER_APPROVED, ContractNegotiationStates.REQUESTED, ContractNegotiationStates.CONSUMER_OFFERED, ContractNegotiationStates.PROVIDER_OFFERED);
    }

    private void checkState(int... legalStates) {
        for (var legalState : legalStates) {
            if (state == legalState) {
                return;
            }
        }
        var values = Arrays.stream(legalStates).mapToObj(String::valueOf).collect(joining(","));
        throw new IllegalStateException(format("Illegal state: %s. Expected one of: %s.", this.state, values));
    }

    public void transitionError(@Nullable String errorDetail) {
        state = ContractNegotiationStates.ERROR.code();
        this.errorDetail = errorDetail;
        stateCount = 1;
        updateStateTimestamp();
    }


    public void rollbackState(ContractNegotiationStates state) {
        this.state = state.code();
        stateCount = 1;
        updateStateTimestamp();
    }

    public ContractNegotiation copy() {
        return ContractNegotiation.Builder.newInstance().id(id).correlationId(correlationId).counterPartyId(counterPartyId)
                .counterPartyAddress(counterPartyAddress).protocol(protocol).type(type).state(state).stateCount(stateCount)
                .stateTimestamp(stateTimestamp).errorDetail(errorDetail).contractAgreement(contractAgreement).contractOffers(contractOffers).build();
    }

    public void updateStateTimestamp() {
        stateTimestamp = Instant.now().toEpochMilli();
    }

    private void transition(ContractNegotiationStates end, ContractNegotiationStates... starts) {
        if (Arrays.stream(starts).noneMatch(s -> s.code() == state)) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", ContractNegotiationStates.from(state), ContractNegotiationStates.from(end.code())));
        }
        stateCount = state == end.code() ? stateCount + 1 : 1;
        state = end.code();
        updateStateTimestamp();
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
        return id.equals(that.id);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final ContractNegotiation negotiation;

        private Builder() {
            negotiation = new ContractNegotiation();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            negotiation.id = id;
            return this;
        }

        public Builder protocol(String protocol) {
            negotiation.protocol = protocol;
            return this;
        }

        public Builder state(int state) {
            negotiation.state = state;
            return this;
        }

        public Builder stateCount(int stateCount) {
            negotiation.stateCount = stateCount;
            return this;
        }

        public Builder stateTimestamp(long stateTimestamp) {
            negotiation.stateTimestamp = stateTimestamp;
            return this;
        }

        public Builder counterPartyId(String id) {
            negotiation.counterPartyId = id;
            return this;
        }

        public Builder counterPartyAddress(String address) {
            negotiation.counterPartyAddress = address;
            return this;
        }

        public Builder correlationId(String id) {
            negotiation.correlationId = id;
            return this;
        }

        public Builder contractAgreement(ContractAgreement agreement) {
            negotiation.contractAgreement = agreement;
            return this;
        }

        //used mainly for JSON deserialization
        public Builder contractOffers(List<ContractOffer> contractOffers) {
            negotiation.contractOffers = contractOffers;
            return this;
        }

        public Builder type(Type type) {
            negotiation.type = type;
            return this;
        }

        public Builder errorDetail(String errorDetail) {
            negotiation.errorDetail = errorDetail;
            return this;
        }

        public Builder contractAgreement(ContractAgreement contractAgreement) {
            negotiation.contractAgreement = contractAgreement;
            return this;
        }

        public Builder contractOffers(List<ContractOffer> contractOffers) {
            negotiation.contractOffers = contractOffers;
            return this;
        }

        public ContractNegotiation build() {
            Objects.requireNonNull(negotiation.id);
            Objects.requireNonNull(negotiation.counterPartyId);
            Objects.requireNonNull(negotiation.counterPartyAddress);
            Objects.requireNonNull(negotiation.protocol);
            if (Type.PROVIDER == negotiation.type) {
                Objects.requireNonNull(negotiation.correlationId);
            }
            return negotiation;
        }
    }
}
