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
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.edc.spi.event.contractnegotiation;

/**
 * This event is raised when the ContractNegotiation has been approved.
 */
public class ContractNegotiationApproved extends ContractNegotiationEvent<ContractNegotiationApproved.Payload> {

    private ContractNegotiationApproved() {
    }

    /**
     * This class contains all event specific attributes of a ContractNegotiation Approved Event
     *
     */
    public static class Payload extends ContractNegotiationEvent.Payload {
    }

    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationApproved, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractNegotiationApproved(), new Payload());
        }
    }

}
