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

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.QueryValidator;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

import java.util.stream.Stream;

import static java.lang.String.format;

public class ContractAgreementServiceImpl implements ContractAgreementService {
    private final ContractNegotiationStore store;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;

    public ContractAgreementServiceImpl(ContractNegotiationStore store, TransactionContext transactionContext) {
        this.store = store;
        this.transactionContext = transactionContext;
        queryValidator = new QueryValidator(ContractAgreement.class);
    }

    @Override
    public ContractAgreement findById(String contractAgreementId) {
        return transactionContext.execute(() -> store.findContractAgreement(contractAgreementId));
    }

    @Override
    public ServiceResult<Stream<ContractAgreement>> query(QuerySpec query) {
        var result = queryValidator.validate(query);

        if (result.failed()) {
            return ServiceResult.badRequest(format("Error validating schema: %s", result.getFailureDetail()));
        }

        return ServiceResult.success(transactionContext.execute(() -> store.queryAgreements(query)));
    }
}
