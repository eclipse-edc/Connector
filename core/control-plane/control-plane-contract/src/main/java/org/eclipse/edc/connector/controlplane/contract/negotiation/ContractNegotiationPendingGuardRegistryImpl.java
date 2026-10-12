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

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuardRegistry;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContractNegotiationPendingGuardRegistryImpl implements ContractNegotiationPendingGuardRegistry {

    private final List<ContractNegotiationPendingGuard> guards = new CopyOnWriteArrayList<>();

    @Override
    public void register(ContractNegotiationPendingGuard guard) {
        guards.add(guard);
    }

    @Override
    public boolean test(ContractNegotiation negotiation) {
        return guards.stream().anyMatch(guard -> guard.test(negotiation));
    }
}
