/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class PolicyServiceImpl implements PolicyService {

    private final TransactionContext transactionContext;
    private final PolicyStore policyStore;
    private final ContractDefinitionStore contractDefinitionStore;

    public PolicyServiceImpl(TransactionContext transactionContext, PolicyStore policyStore, ContractDefinitionStore contractDefinitionStore) {
        this.transactionContext = transactionContext;
        this.policyStore = policyStore;
        this.contractDefinitionStore = contractDefinitionStore;
    }

    @Override
    public Policy findById(String policyId) {
        return transactionContext.execute(() ->
                policyStore.findById(policyId));
    }

    @Override
    public @NotNull Collection<Policy> query(QuerySpec query) {
        return transactionContext.execute(() ->
                policyStore.findAll(query).collect(toList()));
    }

    @Override
    public @NotNull ServiceResult<Policy> deleteById(String policyId) {

        var contractFilter = format("contractPolicy.uid = %s ", policyId);
        var accessFilter = format("accessPolicy.uid = %s ", policyId);

        return transactionContext.execute(() -> {

            if (policyStore.findById(policyId) == null) {
                return ServiceResult.notFound(format("Policy %s does not exist", policyId));
            }

            var queryContractPolicyFilter = QuerySpec.Builder.newInstance().filter(contractFilter).build();
            var contractDefinitionOnPolicy = contractDefinitionStore.findAll(queryContractPolicyFilter);
            if (contractDefinitionOnPolicy.findAny().isPresent()) {
                return ServiceResult.conflict(format("Policy %s cannot be deleted as it is referenced by at least one contract policy", policyId));
            }

            var queryAccessPolicyFilter = QuerySpec.Builder.newInstance().filter(accessFilter).build();
            var accessDefinitionOnPolicy = contractDefinitionStore.findAll(queryAccessPolicyFilter);
            if (accessDefinitionOnPolicy.findAny().isPresent()) {
                return ServiceResult.conflict(format("Policy %s cannot be deleted as it is referenced by at least one access policy", policyId));
            }

            var deleted = policyStore.deleteById(policyId);
            if (deleted == null) {
                return ServiceResult.notFound(format("Policy %s cannot be deleted because it does not exist", policyId));
            }

            return ServiceResult.success(deleted);
        });
    }

    @Override
    public @NotNull ServiceResult<Policy> create(Policy policy) {

        return transactionContext.execute(() -> {
            if (policyStore.findById(policy.getUid()) == null) {
                policyStore.save(policy);
                return ServiceResult.success(policy);
            } else {
                return ServiceResult.conflict(format("Policy %s cannot be created because it already exists", policy.getUid()));
            }
        });
    }
}
