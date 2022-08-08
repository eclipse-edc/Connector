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
import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.MultiplicityConstraint;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.observe.policydefinition.PolicyDefinitionObservable;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.QueryValidator;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class PolicyDefinitionServiceImpl implements PolicyDefinitionService {

    private final TransactionContext transactionContext;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionObservable observable;
    private final QueryValidator queryValidator;

    public PolicyDefinitionServiceImpl(TransactionContext transactionContext, PolicyDefinitionStore policyStore,
                                       ContractDefinitionStore contractDefinitionStore, PolicyDefinitionObservable observable) {
        this.transactionContext = transactionContext;
        this.policyStore = policyStore;
        this.contractDefinitionStore = contractDefinitionStore;
        this.observable = observable;
        queryValidator = new QueryValidator(PolicyDefinition.class, getSubtypeMap());
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        return transactionContext.execute(() ->
                policyStore.findById(policyId));
    }

    @Override
    public ServiceResult<Collection<PolicyDefinition>> query(QuerySpec query) {
        var result = queryValidator.validate(query);

        if (result.failed()) {
            return ServiceResult.badRequest(format("Error validating schema: %s", result.getFailureDetail()));
        }
        return ServiceResult.success(transactionContext.execute(() ->
                policyStore.findAll(query).collect(toList())));
    }


    @Override
    public @NotNull ServiceResult<PolicyDefinition> deleteById(String policyId) {

        var contractFilter = format("contractPolicyId = %s ", policyId);
        var accessFilter = format("accessPolicyId = %s ", policyId);

        return transactionContext.execute(() -> {

            if (policyStore.findById(policyId) == null) {
                return ServiceResult.notFound(format("PolicyDefinition %s does not exist", policyId));
            }

            var queryContractPolicyFilter = QuerySpec.Builder.newInstance().filter(contractFilter).build();
            var contractDefinitionOnPolicy = contractDefinitionStore.findAll(queryContractPolicyFilter);
            if (contractDefinitionOnPolicy.findAny().isPresent()) {
                return ServiceResult.conflict(format("PolicyDefinition %s cannot be deleted as it is referenced by at least one contract definition", policyId));
            }

            var queryAccessPolicyFilter = QuerySpec.Builder.newInstance().filter(accessFilter).build();
            var accessDefinitionOnPolicy = contractDefinitionStore.findAll(queryAccessPolicyFilter);
            if (accessDefinitionOnPolicy.findAny().isPresent()) {
                return ServiceResult.conflict(format("PolicyDefinition %s cannot be deleted as it is referenced by at least one contract definition", policyId));
            }

            var deleted = policyStore.deleteById(policyId);
            if (deleted == null) {
                return ServiceResult.notFound(format("PolicyDefinition %s cannot be deleted because it does not exist", policyId));
            }

            observable.invokeForEach(l -> l.deleted(deleted));
            return ServiceResult.success(deleted);
        });
    }

    @Override
    public @NotNull ServiceResult<PolicyDefinition> create(PolicyDefinition policyDefinition) {

        return transactionContext.execute(() -> {
            if (policyStore.findById(policyDefinition.getUid()) == null) {
                policyStore.save(policyDefinition);
                observable.invokeForEach(l -> l.created(policyDefinition));
                return ServiceResult.success(policyDefinition);
            } else {
                return ServiceResult.conflict(format("PolicyDefinition %s cannot be created because it already exists", policyDefinition.getUid()));
            }
        });
    }

    private Map<Class<?>, List<Class<?>>> getSubtypeMap() {
        return Map.of(
                Constraint.class, List.of(MultiplicityConstraint.class, AtomicConstraint.class),
                MultiplicityConstraint.class, List.of(AndConstraint.class, OrConstraint.class, XoneConstraint.class),
                Expression.class, List.of(LiteralExpression.class)
        );
    }
}
