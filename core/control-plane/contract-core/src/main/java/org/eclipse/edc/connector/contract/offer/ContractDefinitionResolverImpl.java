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

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
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
public class ContractDefinitionResolverImpl implements ContractDefinitionResolver {
    private final PolicyEngine policyEngine;
    private final PolicyDefinitionStore policyStore;
    private final Monitor monitor;
    private final ContractDefinitionStore definitionStore;

    public ContractDefinitionResolverImpl(Monitor monitor, ContractDefinitionStore contractDefinitionStore, PolicyEngine policyEngine, PolicyDefinitionStore policyStore) {
        this.monitor = monitor;
        definitionStore = contractDefinitionStore;
        this.policyEngine = policyEngine;
        this.policyStore = policyStore;
    }

    @NotNull
    @Override
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return definitionStore.findAll(QuerySpec.max())
                .filter(definition -> evaluateAccessPolicy(definition, agent));
    }

    @Nullable
    @Override
    public ContractDefinition definitionFor(ParticipantAgent agent, String definitionId) {
        return Optional.of(definitionId)
                .map(definitionStore::findById)
                .filter(definition -> evaluateAccessPolicy(definition, agent))
                .orElse(null);
    }

    /**
     * Determines the applicability of a definition to an agent by evaluating its access policy.
     */
    private boolean evaluateAccessPolicy(ContractDefinition definition, ParticipantAgent agent) {
        var accessResult = Optional.of(definition.getAccessPolicyId())
                .map(policyStore::findById)
                .map(PolicyDefinition::getPolicy)
                .map(policy -> policyEngine.evaluate(CATALOGING_SCOPE, policy, agent))
                .orElse(Result.failure(format("Policy %s not found", definition.getAccessPolicyId())));

        if (accessResult.failed()) {
            monitor.debug(format("Access not granted for %s: \n%s", definition.getId(), String.join("\n", accessResult.getFailureMessages())));
            return false;
        }

        return true;
    }
}
