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

package org.eclipse.edc.connector.controlplane.services.contractagreement;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class ContractAgreementServiceImpl implements ContractAgreementService {
    private final ContractNegotiationStore store;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;

    public ContractAgreementServiceImpl(ContractNegotiationStore store, TransactionContext transactionContext, QueryValidator queryValidator) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.queryValidator = queryValidator;
    }

    @Override
    public ContractAgreement findById(String contractAgreementId) {
        return transactionContext.execute(() -> store.findContractAgreement(contractAgreementId));
    }

    @Override
    public ServiceResult<List<ContractAgreement>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(queryAgreements(query))
                );
    }

    @Override
    public ContractNegotiation findNegotiation(String contractAgreementId) {
        var criterion = criterion("contractAgreement.id", "=", contractAgreementId);
        var query = QuerySpec.Builder.newInstance().filter(criterion).build();
        return transactionContext.execute(() -> store.queryNegotiations(query).findFirst().orElse(null));
    }

    private List<ContractAgreement> queryAgreements(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = store.queryAgreements(query)) {
                return stream.toList();
            }
        });
    }
}
