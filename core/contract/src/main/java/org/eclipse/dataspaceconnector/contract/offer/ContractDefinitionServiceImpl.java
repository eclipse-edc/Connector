/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Microsoft Corporation - Refactoring
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Determines the contract definitions applicable to a {@link ParticipantAgent} by evaluating the access control and
 * usage policies associated with a set of assets as defined by {@link ContractDefinition}s. On the distinction between
 * access control and usage policy, see {@link ContractDefinition}.
 */
public class ContractDefinitionServiceImpl implements ContractDefinitionService {
    private final PolicyEngine policyEngine;
    private final PolicyDefinitionStore policyStore;
    private final Monitor monitor;
    private final ContractDefinitionStore definitionStore;

    public ContractDefinitionServiceImpl(Monitor monitor, ContractDefinitionStore contractDefinitionStore, PolicyEngine policyEngine, PolicyDefinitionStore policyStore) {
        this.monitor = monitor;
        definitionStore = contractDefinitionStore;
        this.policyEngine = policyEngine;
        this.policyStore = policyStore;
    }

    @NotNull
    @Override
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return definitionStore.findAll().stream()
                .filter(definition -> evaluatePolicies(definition, agent));
    }

    @Nullable
    @Override
    public ContractDefinition definitionFor(ParticipantAgent agent, String definitionId) {
        return Optional.of(definitionId)
                .map(definitionStore::findById)
                .filter(definition -> evaluatePolicies(definition, agent))
                .orElse(null);
    }

    /**
     * Determines the applicability of a definition to an agent by evaluating the union of its access control and usage
     * policies.
     */
    private boolean evaluatePolicies(ContractDefinition definition, ParticipantAgent agent) {
        var accessResult = evaluate(definition.getAccessPolicyId(), agent);

        if (accessResult.failed()) {
            monitor.info(format("Problem evaluating access control policy for %s: \n%s", definition.getId(), String.join("\n", accessResult.getFailureMessages())));
            return false;
        }

        var controlResult = evaluate(definition.getContractPolicyId(), agent);

        if (controlResult.failed()) {
            monitor.info(format("Problem evaluating usage control policy for %s: \n%s", definition.getId(), String.join("\n", controlResult.getFailureMessages())));
            return false;
        }

        return true;
    }

    @NotNull
    private Result<Policy> evaluate(String policyId, ParticipantAgent agent) {
        return Optional.of(policyId)
                .map(policyStore::findById)
                .map(PolicyDefinition::getPolicy)
                .map(policy -> policyEngine.evaluate(NEGOTIATION_SCOPE, policy, agent))
                .orElse(Result.failure(format("Policy %s not found", policyId)));
    }
}
