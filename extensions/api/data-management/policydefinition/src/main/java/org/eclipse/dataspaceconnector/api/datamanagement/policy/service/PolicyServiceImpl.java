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
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class PolicyServiceImpl implements PolicyService {

    private final TransactionContext transactionContext;
    private final PolicyStore policyStore;
    private final ContractDefinitionStore contractDefinitionStore;

    public PolicyServiceImpl(TransactionContext transactionContext, PolicyStore policyStore, AssetIndex index, ContractDefinitionStore contractDefinitionStore) {
        this.transactionContext = transactionContext;
        this.policyStore = policyStore;
        this.contractDefinitionStore = contractDefinitionStore;
    }

    @Override
    public Policy findbyId(String policyId) {
        return policyStore.findById(policyId);
    }

    @Override
    public Collection<Policy> query(QuerySpec query) {
        return policyStore.findAll(query).collect(toList());
    }

    @Override
    public Policy delete(String policyId) {

        var result = new AtomicReference<ServiceResult<Policy>>();
        var filter = format("contractPolicy.uid = %s ", policyId);


        var query = QuerySpec.Builder.newInstance().filter(filter).build();

        //transactionContext.execute(() -> {
        //    var filter = format("contractAgreement.asset.properties.%s = %s", PROPERTY_ID, assetId);
        //}


        return policyStore.delete(policyId);
    }

    @Override
    public void create(Policy policy) {

        var result = new AtomicReference<ServiceResult<Policy>>();

        transactionContext.execute(() -> {
            if (policyStore.findById(policy.getUid()) == null) {
                policyStore.save(policy);
                result.set(ServiceResult.success(policy));
            } else {
                result.set(ServiceResult.conflict(format("Policy %s cannot be created because it already exist", policy.getUid())));
            }
        });
        result.set(ServiceResult.success(policy));
    }
}
