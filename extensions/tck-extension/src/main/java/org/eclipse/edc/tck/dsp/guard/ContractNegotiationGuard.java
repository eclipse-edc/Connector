/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.persistence.StateEntityStore;

import java.util.Set;
import java.util.function.Consumer;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;

/**
 * Contract negotiation guard for TCK testcases.
 */
public class ContractNegotiationGuard extends DelayedActionGuard<ContractNegotiation> implements ContractNegotiationPendingGuard {
    // the states to not apply the guard to - i.e. to allow automatic transitions by the contract negotiation manager
    private static final Set<Integer> PROVIDER_AUTOMATIC_STATES = Set.of(
            OFFERING.code(),
            AGREEING.code(),
            TERMINATING.code(),
            FINALIZING.code());

    private static final Set<Integer> CONSUMER_AUTOMATIC_STATES = Set.of(
            INITIAL.code(),
            REQUESTING.code(),
            ACCEPTING.code(),
            VERIFYING.code()
    );

    public ContractNegotiationGuard(Consumer<ContractNegotiation> action, StateEntityStore<ContractNegotiation> store) {
        super(cn -> cn.getType() == PROVIDER ?
                !PROVIDER_AUTOMATIC_STATES.contains(cn.getState()) : !CONSUMER_AUTOMATIC_STATES.contains(cn.getState()), action, store);
    }
}
