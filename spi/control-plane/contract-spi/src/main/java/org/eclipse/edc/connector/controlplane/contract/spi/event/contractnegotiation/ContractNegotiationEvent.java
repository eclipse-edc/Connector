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
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.event.CallbackAddresses;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type ContractNegotiation to catch them together in an Event Subscriber
 *  Contains data related to contract negotiations
 */
public abstract class ContractNegotiationEvent extends Event implements CallbackAddresses {

    protected String contractNegotiationId;

    protected String counterPartyAddress;
    protected String counterPartyId;

    protected List<CallbackAddress> callbackAddresses = new ArrayList<>();
    protected List<ContractOffer> contractOffers = new ArrayList<>();
    protected String protocol;

    public String getContractNegotiationId() {
        return contractNegotiationId;
    }


    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public List<ContractOffer> getContractOffers() {
        return contractOffers;
    }

    public String getProtocol() {
        return protocol;
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

    @Override
    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public abstract static class Builder<T extends ContractNegotiationEvent, B extends ContractNegotiationEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B contractNegotiationId(String contractNegotiationId) {
            event.contractNegotiationId = contractNegotiationId;
            return self();
        }

        public B counterPartyAddress(String counterPartyAddress) {
            event.counterPartyAddress = counterPartyAddress;
            return self();
        }

        public B counterPartyId(String counterPartyId) {
            event.counterPartyId = counterPartyId;
            return self();
        }

        public B contractOffers(List<ContractOffer> offers) {
            event.contractOffers = offers;
            return self();
        }

        public B callbackAddresses(List<CallbackAddress> callbackAddresses) {
            event.callbackAddresses = callbackAddresses;
            return self();
        }

        public B protocol(String protocol) {
            event.protocol = protocol;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.contractNegotiationId);
            Objects.requireNonNull(event.counterPartyAddress);
            Objects.requireNonNull(event.counterPartyId);
            Objects.requireNonNull(event.protocol);

            return event;
        }
    }
}
