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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - extended method implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - refactor
 *       ZF Friedrichshafen AG - fixed contract validity issue
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.statemachine.StateMachineManager;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;

/**
 * Implementation of the {@link ConsumerContractNegotiationManager}.
 */
public class ConsumerContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ConsumerContractNegotiationManager {

    private ConsumerContractNegotiationManagerImpl() {
    }

    @Override
    ContractNegotiation.Type type() {
        return CONSUMER;
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processNegotiationsInState(INITIAL, negotiationProcessors::processInitial))
                .processor(processNegotiationsInState(REQUESTING, negotiationProcessors::processRequesting))
                .processor(processNegotiationsInState(ACCEPTING, negotiationProcessors::processAccepting))
                .processor(processNegotiationsInState(AGREED, negotiationProcessors::processAgreed))
                .processor(processNegotiationsInState(VERIFYING, negotiationProcessors::processVerifying))
                .processor(processNegotiationsInState(TERMINATING, negotiationProcessors::processTerminating));
    }

    /**
     * Builder for ConsumerContractNegotiationManagerImpl.
     */
    public static class Builder extends AbstractContractNegotiationManager.Builder<ConsumerContractNegotiationManagerImpl> {

        private Builder() {
            super(new ConsumerContractNegotiationManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

    }
}
