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

package org.eclipse.edc.spi.event.contractnegotiation;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type ContractNegotiation to catch them together in an Event Subscriber
 *  Contains data related to contract negotiations
 */
public abstract class ContractNegotiationEvent extends Event {

    protected String contractNegotiationId;

    protected List<CallbackAddress> callbackAddresses = new ArrayList<>();

    public String getContractNegotiationId() {
        return contractNegotiationId;
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

        public B callbackAddresses(List<CallbackAddress> callbackAddresses) {
            event.callbackAddresses = callbackAddresses;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.contractNegotiationId);
            return event;
        }
    }
}
