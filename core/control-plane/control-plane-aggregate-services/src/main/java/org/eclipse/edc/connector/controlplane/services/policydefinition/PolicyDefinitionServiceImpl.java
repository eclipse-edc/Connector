/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.policydefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.observe.PolicyDefinitionObservable;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class PolicyDefinitionServiceImpl implements PolicyDefinitionService {

    private final TransactionContext transactionContext;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionObservable observable;
    private final QueryValidator queryValidator;

    public PolicyDefinitionServiceImpl(TransactionContext transactionContext, PolicyDefinitionStore policyStore,
                                       ContractDefinitionStore contractDefinitionStore, PolicyDefinitionObservable observable) {
        this(transactionContext, policyStore, contractDefinitionStore, observable,
                new QueryValidator(PolicyDefinition.class, new PolicyDefinitionServiceSubtypesProvider().getSubtypeMap()));
    }

    public PolicyDefinitionServiceImpl(TransactionContext transactionContext, PolicyDefinitionStore policyStore,
                                       ContractDefinitionStore contractDefinitionStore, PolicyDefinitionObservable observable, QueryValidator queryValidator) {
        this.transactionContext = transactionContext;
        this.policyStore = policyStore;
        this.contractDefinitionStore = contractDefinitionStore;
        this.observable = observable;
        this.queryValidator = queryValidator;
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        return transactionContext.execute(() ->
                policyStore.findById(policyId));
    }

    @Override
    public ServiceResult<List<PolicyDefinition>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(queryPolicyDefinitions(query))
                );
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> deleteById(String policyId) {
        return transactionContext.execute(() -> {

            var contractFilter = criterion("contractPolicyId", "=", policyId);
            var accessFilter = criterion("accessPolicyId", "=", policyId);

            var queryContractPolicyFilter = QuerySpec.Builder.newInstance().filter(contractFilter).build();
            try (var contractDefinitionOnPolicy = contractDefinitionStore.findAll(queryContractPolicyFilter)) {
                if (contractDefinitionOnPolicy.findAny().isPresent()) {
                    return ServiceResult.conflict(format("PolicyDefinition %s cannot be deleted as it is referenced by at least one contract definition", policyId));
                }
            }

            var queryAccessPolicyFilter = QuerySpec.Builder.newInstance().filter(accessFilter).build();
            try (var accessDefinitionOnPolicy = contractDefinitionStore.findAll(queryAccessPolicyFilter)) {
                if (accessDefinitionOnPolicy.findAny().isPresent()) {
                    return ServiceResult.conflict(format("PolicyDefinition %s cannot be deleted as it is referenced by at least one contract definition", policyId));
                }
            }

            var deleted = policyStore.delete(policyId);
            deleted.onSuccess(pd -> observable.invokeForEach(l -> l.deleted(pd)));
            return ServiceResult.from(deleted);
        });
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> create(PolicyDefinition policyDefinition) {
        return transactionContext.execute(() -> {
            var saveResult = policyStore.create(policyDefinition);
            saveResult.onSuccess(v -> observable.invokeForEach(l -> l.created(policyDefinition)));
            return ServiceResult.from(saveResult);
        });
    }


    @Override
    public ServiceResult<PolicyDefinition> update(PolicyDefinition policyDefinition) {
        return transactionContext.execute(() -> {
            var updateResult = policyStore.update(policyDefinition);
            updateResult.onSuccess(p -> observable.invokeForEach(l -> l.updated(p)));
            return ServiceResult.from(updateResult);
        });
    }

    private List<PolicyDefinition> queryPolicyDefinitions(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = policyStore.findAll(query)) {
                return stream.toList();
            }
        });
    }
}