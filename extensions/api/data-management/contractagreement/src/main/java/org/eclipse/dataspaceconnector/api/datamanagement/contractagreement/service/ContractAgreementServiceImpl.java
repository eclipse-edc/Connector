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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.service;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class ContractAgreementServiceImpl implements ContractAgreementService {
    private final ContractNegotiationStore store;
    private final TransactionContext transactionContext;

    public ContractAgreementServiceImpl(ContractNegotiationStore store, TransactionContext transactionContext) {
        this.store = store;
        this.transactionContext = transactionContext;
    }

    @Override
    public ContractAgreement findById(String contractAgreementId) {
        return transactionContext.execute(() -> store.findContractAgreement(contractAgreementId));
    }

    @Override
    public Collection<ContractAgreement> query(QuerySpec query) {
        return transactionContext.execute(() -> store.queryAgreements(query).collect(toList()));
    }
}
