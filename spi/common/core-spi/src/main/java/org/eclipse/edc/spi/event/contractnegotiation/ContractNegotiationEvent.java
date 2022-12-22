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
import org.eclipse.edc.spi.event.EventPayload;

import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type ContractNegotiation to catch them together in an Event Subscriber
 *  Contains data related to contract negotiations
 */
public abstract class ContractNegotiationEvent<P extends ContractNegotiationEvent.Payload> extends Event<P> {

    public static abstract class Payload extends EventPayload {
        protected String contractNegotiationId;

        public String getContractNegotiationId() {
            return contractNegotiationId;
        }
    }

    public static class Builder<E extends ContractNegotiationEvent<P>, P extends ContractNegotiationEvent.Payload, B extends Builder<E, P, B>> extends Event.Builder<E, P, B> {

        protected Builder(E event, P payload) {
            super(event, payload);
        }

        @SuppressWarnings("unchecked")
        public B contractNegotiationId(String contractNegotiationId) {
            event.payload.contractNegotiationId = contractNegotiationId;
            return (B) this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.contractNegotiationId);
        }

    }
}
