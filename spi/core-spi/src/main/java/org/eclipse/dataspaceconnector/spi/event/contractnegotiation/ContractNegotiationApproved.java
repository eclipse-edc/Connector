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

package org.eclipse.dataspaceconnector.spi.event.contractnegotiation;

import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventPayload;

import java.util.Objects;

/**
 * This event is raised when the ContractNegotiation has been approved.
 */
public class ContractNegotiationApproved extends Event<ContractNegotiationApproved.Payload> {

    private ContractNegotiationApproved() {
    }

    public static class Builder extends Event.Builder<ContractNegotiationApproved, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractNegotiationApproved(), new Payload());
        }

        public Builder contractNegotiationId(String contractNegotiationId) {
            event.payload.contractNegotiationId = contractNegotiationId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.contractNegotiationId);
        }
    }

    public static class Payload extends EventPayload {
        private String contractNegotiationId;

        public String getContractNegotiationId() {
            return contractNegotiationId;
        }
    }
}
