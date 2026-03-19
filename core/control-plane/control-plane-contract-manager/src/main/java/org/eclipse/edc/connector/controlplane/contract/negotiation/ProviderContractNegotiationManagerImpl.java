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
 *       Cofinity-X - add participantId to DataspaceProfileContext
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.statemachine.StateMachineManager;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;

/**
 * Implementation of the {@link ProviderContractNegotiationManager}.
 */
public class ProviderContractNegotiationManagerImpl extends AbstractContractNegotiationManager implements ProviderContractNegotiationManager {

    private ProviderContractNegotiationManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processNegotiationsInState(OFFERING, negotiationProcessors::processOffering))
                .processor(processNegotiationsInState(REQUESTED, negotiationProcessors::processRequested))
                .processor(processNegotiationsInState(ACCEPTED, negotiationProcessors::processAccepted))
                .processor(processNegotiationsInState(AGREEING, negotiationProcessors::processAgreeing))
                .processor(processNegotiationsInState(VERIFIED, negotiationProcessors::processVerified))
                .processor(processNegotiationsInState(FINALIZING, negotiationProcessors::processFinalizing))
                .processor(processNegotiationsInState(TERMINATING, negotiationProcessors::processTerminating));
    }

    @Override
    protected ContractNegotiation.Type type() {
        return PROVIDER;
    }

    public static class Builder extends AbstractContractNegotiationManager.Builder<ProviderContractNegotiationManagerImpl> {

        private Builder() {
            super(new ProviderContractNegotiationManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

    }
}
