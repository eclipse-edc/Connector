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
 */
package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionResult;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Determines the contract definitions applicable to a {@link ParticipantAgent} by evaluating the access control and usage policies associated with a set of assets as defined by
 * {@link ContractDefinition}s. On the distinction between access control and usage policy, see {@link ContractDefinition}.
 */
public class ContractDefinitionServiceImpl implements ContractDefinitionService {
    private final PolicyEngine policyEngine;
    private final Monitor monitor;

    private List<ContractDefinition> definitions = new CopyOnWriteArrayList<>();

    public ContractDefinitionServiceImpl(PolicyEngine policyEngine, Monitor monitor) {
        this.policyEngine = policyEngine;
        this.monitor = monitor;
    }

    @NotNull
    @Override
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return definitions.stream().filter(definition -> evaluatePolicies(definition, agent));
    }

    @NotNull
    public ContractDefinitionResult definitionFor(ParticipantAgent agent, String definitionId) {
        var definitionOptional = definitions.stream().filter(d -> d.getId().equals(definitionId)).findFirst();
        if (definitionOptional.isPresent()) {
            var definition = definitionOptional.get();
            if (evaluatePolicies(definition, agent)) {
                return new ContractDefinitionResult(definition);
            }
        }
        return ContractDefinitionResult.INVALID;
    }

    /**
     * Loads {@link ContractDefinition}s into the system.
     */
    public void load(Collection<ContractDefinition> definitions) {
        this.definitions.addAll(definitions);
    }

    /**
     * Removes definitions matching the collection of ids.
     */
    public void remove(Collection<String> definitionIds) {
        definitionIds.forEach(id -> this.definitions.removeIf(definition -> definition.getId().equals(id)));
    }

    /**
     * Returns the active definitions.
     */
    public @NotNull List<ContractDefinition> getLoadedDefinitions() {
        return definitions;
    }

    /**
     * Determines the applicability of a definition to an agent by evaluating the union of its access control and usage policies.
     */
    private boolean evaluatePolicies(ContractDefinition definition, ParticipantAgent agent) {
        var accessResult = policyEngine.evaluate(definition.getAccessPolicy(), agent);
        if (!accessResult.valid()) {
            monitor.info(format("Problem evaluating access control policy for %s: \n%s", definition.getId(), String.join("\n", accessResult.getProblems())));
            return false;
        }
        var usageResult = policyEngine.evaluate(definition.getContractPolicy(), agent);
        if (!usageResult.valid()) {
            monitor.info(format("Problem evaluating usage control policy for %s: \n%s", definition.getId(), String.join("\n", accessResult.getProblems())));
            return false;
        }
        return true;
    }
}
