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

package org.eclipse.edc.connector.controlplane.services.contractdefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ContractDefinitionServiceImpl implements ContractDefinitionService {
    private final ContractDefinitionStore store;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final TransactionContext transactionContext;
    private final ContractDefinitionObservable observable;
    private final QueryValidator queryValidator;

    public ContractDefinitionServiceImpl(ContractDefinitionStore store, PolicyDefinitionStore policyDefinitionStore,
                                         TransactionContext transactionContext, ContractDefinitionObservable observable,
                                         QueryValidator queryValidator) {
        this.store = store;
        this.policyDefinitionStore = policyDefinitionStore;
        this.transactionContext = transactionContext;
        this.observable = observable;
        this.queryValidator = queryValidator;
    }

    @Override
    public ContractDefinition findById(String contractDefinitionId) {
        return transactionContext.execute(() -> store.findById(contractDefinitionId));
    }

    @Override
    public ServiceResult<List<ContractDefinition>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(queryContractDefinitions(query))
                );
    }

    @Override
    public ServiceResult<ContractDefinition> create(ContractDefinition contractDefinition) {
        return transactionContext.execute(() -> {
            var policiesValidation = validatePoliciesOwnership(contractDefinition);
            if (policiesValidation.failed()) {
                return policiesValidation.mapFailure();
            }

            var saveResult = store.save(contractDefinition);
            if (saveResult.succeeded()) {
                observable.invokeForEach(l -> l.created(contractDefinition));
                return ServiceResult.success(contractDefinition);
            } else {
                return ServiceResult.fromFailure(saveResult);
            }
        });
    }

    @Override
    public ServiceResult<Void> update(ContractDefinition contractDefinition) {
        return transactionContext.execute(() -> {
            var policiesValidation = validatePoliciesOwnership(contractDefinition);
            if (policiesValidation.failed()) {
                return policiesValidation;
            }

            var updateResult = store.update(contractDefinition);
            var serviceResult = ServiceResult.from(updateResult);
            serviceResult.onSuccess(a -> observable.invokeForEach(l -> l.updated(contractDefinition)));
            return serviceResult;
        });
    }

    @Override
    public ServiceResult<ContractDefinition> delete(String contractDefinitionId) {
        return transactionContext.execute(() -> {
            var deleteResult = store.deleteById(contractDefinitionId);
            var serviceResult = ServiceResult.from(deleteResult);

            serviceResult.onSuccess(deleted -> observable.invokeForEach(l -> l.deleted(deleted)));
            return serviceResult;
        });
    }

    /**
     * The referenced policies, when they exist, must belong to the same participant context as the contract definition,
     * otherwise a participant could evaluate or expose policies owned by another participant.
     */
    private ServiceResult<Void> validatePoliciesOwnership(ContractDefinition contractDefinition) {
        var participantContextId = contractDefinition.getParticipantContextId();
        return Stream.of(contractDefinition.getAccessPolicyId(), contractDefinition.getContractPolicyId())
                .filter(Objects::nonNull)
                .distinct()
                .map(policyId -> {
                    var policy = policyDefinitionStore.findById(policyId);
                    if (policy != null && !Objects.equals(policy.getParticipantContextId(), participantContextId)) {
                        return ServiceResult.<Void>badRequest(format("Policy %s does not belong to participant context %s", policyId, participantContextId));
                    }
                    return ServiceResult.<Void>success();
                })
                .filter(ServiceResult::failed)
                .findFirst()
                .orElse(ServiceResult.success());
    }

    private List<ContractDefinition> queryContractDefinitions(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = store.findAll(query)) {
                return stream.toList();
            }
        });
    }
}
