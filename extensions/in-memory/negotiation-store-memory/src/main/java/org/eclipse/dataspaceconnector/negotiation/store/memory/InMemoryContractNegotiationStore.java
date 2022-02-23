/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore implements ContractNegotiationStore {
    private final Map<String, ContractNegotiation> negotiations = new ConcurrentHashMap<>();

    @Override
    public ContractNegotiation find(String id) {
        return negotiations.get(id);
    }

    @Override
    public @Nullable ContractNegotiation findContractOfferByLatestMessageId(String contractOfferMessageId) {
        return negotiations.values().stream().filter(n -> Objects.equals(n.getLastContractOffer().getProperty(ContractOffer.PROPERTY_MESSAGE_ID), contractOfferMessageId)).findFirst().orElse(null);
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        return negotiations.values().stream().map(ContractNegotiation::getContractAgreement).filter(contractAgreement -> contractAgreement.getId().equals(contractId)).findFirst().orElse(null);
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        ContractNegotiation newNegotiation = negotiation.copy();
        newNegotiation.updateStateTimestamp();
        negotiations.put(newNegotiation.getId(), newNegotiation);
    }

    @Override
    public void delete(String processId) {
        negotiations.remove(processId);
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
        return negotiations.values().stream()
                .filter(n -> n.getState() == state)
                .sorted(Comparator.comparingLong(ContractNegotiation::getStateTimestamp)) //order by state timestamp, oldest timestamp first
                .limit(max)
                .map(ContractNegotiation::copy)
                .collect(toList());
    }
}
