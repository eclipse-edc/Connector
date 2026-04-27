/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.tasks.tck.dsp.recorder.Step;

import java.util.function.Function;

public class ContractNegotiationGuard extends DelayedActionGuard<ContractNegotiation> implements ContractNegotiationPendingGuard {

    public ContractNegotiationGuard(Function<ContractNegotiation, Step<ContractNegotiation>> actionProvider, StateEntityStore<ContractNegotiation> store) {
        super(actionProvider, store);
    }
}
