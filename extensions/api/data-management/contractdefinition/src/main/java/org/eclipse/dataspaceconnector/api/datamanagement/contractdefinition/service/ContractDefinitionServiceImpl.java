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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class ContractDefinitionServiceImpl implements ContractDefinitionService {
    private final ContractDefinitionStore store;
    private final ContractDefinitionLoader loader;
    private final TransactionContext transactionContext;

    public ContractDefinitionServiceImpl(ContractDefinitionStore store, ContractDefinitionLoader loader, TransactionContext transactionContext) {
        this.store = store;
        this.loader = loader;
        this.transactionContext = transactionContext;
    }

    @Override
    public ContractDefinition findById(String contractDefinitionId) {
        var querySpec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("id", "=", contractDefinitionId)))
                .build();

        return transactionContext.execute(() -> store.findAll(querySpec).findFirst().orElse(null));
    }

    @Override
    public Collection<ContractDefinition> query(QuerySpec query) {
        return transactionContext.execute(() -> store.findAll(query).collect(toList()));
    }

    @Override
    public ServiceResult<ContractDefinition> create(ContractDefinition contractDefinition) {
        return transactionContext.execute(() -> {
            if (findById(contractDefinition.getId()) == null) {
                loader.accept(contractDefinition);
                return ServiceResult.success(contractDefinition);
            } else {
                return ServiceResult.conflict(format("ContractDefinition %s cannot be created because it already exist", contractDefinition.getId()));
            }
        });
    }

    @Override
    public ServiceResult<ContractDefinition> delete(String contractDefinitionId) {
        return transactionContext.execute(() -> {
            // TODO: should be checked if a contract agreement based on this definition exists. Currently not implementable because it's not possibile to filter agreements by definition id

            var deleted = store.deleteById(contractDefinitionId);
            if (deleted == null) {
                return ServiceResult.notFound(format("ContractDefinition %s does not exist", contractDefinitionId));
            } else {
                return ServiceResult.success(deleted);
            }

        });
    }
}
