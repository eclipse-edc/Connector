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
 *
 */
package org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Represents a contract negotiation.
 * <p>
 * TODO: This is only placeholder
 * TODO: Implement state transitions
 * TODO: Add error details
 */
@JsonTypeName("dataspaceconnector:contractnegotiation")
@JsonDeserialize(builder = ContractNegotiation.Builder.class)
public class ContractNegotiation {
    private final Type type = Type.CLIENT;
    private List<ContractOffer> contractOffers = new ArrayList<>();
    private String id;
    private String correlationId;
    private String counterPartyId;
    private String protocol;
    private int state;
    private int stateCount;
    private long stateTimestamp;
    private ContractAgreement contractAgreement;

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getCounterPartyId() {
        return counterPartyId;
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

    private void checkState(int... legalStates) {
        for (var legalState : legalStates) {
            if (state == legalState) {
                return;
            }
        }
        var values = Arrays.stream(legalStates).mapToObj(String::valueOf).collect(joining(","));
        throw new IllegalStateException(format("Illegal state: %s. Expected one of: %s.", state, values));
    }

    public enum Type {
        CLIENT, PROVIDER
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

        public ContractNegotiation build() {
            Objects.requireNonNull(negotiation.id);
            Objects.requireNonNull(negotiation.counterPartyId);
            Objects.requireNonNull(negotiation.protocol);
            if (Type.CLIENT == negotiation.type) {
                Objects.requireNonNull(negotiation.correlationId);
            }
            return negotiation;
        }


    }
}
