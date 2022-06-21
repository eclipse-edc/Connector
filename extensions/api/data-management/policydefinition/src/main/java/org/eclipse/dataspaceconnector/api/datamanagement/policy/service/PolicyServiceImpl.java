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

package org.eclipse.dataspaceconnector.api.datamanagement.policy.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class PolicyServiceImpl implements PolicyService {

    private final TransactionContext transactionContext;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore contractDefinitionStore;

    public PolicyServiceImpl(TransactionContext transactionContext, PolicyDefinitionStore policyStore, ContractDefinitionStore contractDefinitionStore) {
        this.transactionContext = transactionContext;
        this.policyStore = policyStore;
        this.contractDefinitionStore = contractDefinitionStore;
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        return transactionContext.execute(() ->
                policyStore.findById(policyId));
    }

    @Override
    public @NotNull Collection<PolicyDefinition> query(QuerySpec query) {
        return transactionContext.execute(() ->
                policyStore.findAll(query).collect(toList()));
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> deleteById(String policyId) {

        return transactionContext.execute(() -> {

            if (policyStore.findById(policyId) == null) {
                return ServiceResult.notFound(format("Policy %s does not exist", policyId));
            }

            var contractDefinitionOnPolicy = contractDefinitionStore.isReferenced(policyId);
            if (contractDefinitionOnPolicy.findAny().isPresent()) {
                return ServiceResult.conflict(format("Policy %s cannot be deleted as it is referenced by at least one contract definition", policyId));
            }


            var deleted = policyStore.deleteById(policyId);
            if (deleted == null) {
                return ServiceResult.notFound(format("Policy %s cannot be deleted because it does not exist", policyId));
            }

            return ServiceResult.success(deleted);
        });
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> create(PolicyDefinition policy) {

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
