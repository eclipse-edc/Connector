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

package org.eclipse.edc.connector.service.contractdefinition;

import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.stream.Stream;

import static java.lang.String.format;

public class ContractDefinitionServiceImpl implements ContractDefinitionService {
    private final ContractDefinitionStore store;
    private final TransactionContext transactionContext;
    private final ContractDefinitionObservable observable;
    private final QueryValidator queryValidator;

    public ContractDefinitionServiceImpl(ContractDefinitionStore store, TransactionContext transactionContext, ContractDefinitionObservable observable) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.observable = observable;
        queryValidator = new QueryValidator(ContractDefinition.class);
    }

    @Override
    public ContractDefinition findById(String contractDefinitionId) {
        return transactionContext.execute(() -> store.findById(contractDefinitionId));
    }

    @Override
    public ServiceResult<Stream<ContractDefinition>> query(QuerySpec query) {
        var result = queryValidator.validate(query);

        if (result.failed()) {
            return ServiceResult.badRequest(format("Error validating schema: %s", result.getFailureDetail()));
        }
        return ServiceResult.success(transactionContext.execute(() -> store.findAll(query)));
    }

    @Override
    public ServiceResult<ContractDefinition> create(ContractDefinition contractDefinition) {
        return transactionContext.execute(() -> {
            if (findById(contractDefinition.getId()) == null) {
                store.accept(contractDefinition);
                observable.invokeForEach(l -> l.created(contractDefinition));
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
                observable.invokeForEach(l -> l.deleted(deleted));
                return ServiceResult.success(deleted);
            }

        });
    }
}
