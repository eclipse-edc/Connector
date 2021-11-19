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
package org.eclipse.dataspaceconnector.demo.contract.service;

import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinition;
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
 * {@link ContractDescriptor}s. On the distinction between access control and usage policy, see {@link ContractDescriptor}.
 */
public class DynamicPolicyContractDefinitionService implements ContractDefinitionService {
    private final PolicyEngine policyEngine;
    private final Monitor monitor;

    private List<ContractDescriptor> descriptors = new CopyOnWriteArrayList<>();

    public DynamicPolicyContractDefinitionService(PolicyEngine policyEngine, Monitor monitor) {
        this.policyEngine = policyEngine;
        this.monitor = monitor;
    }

    @NotNull
    @Override
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return descriptors.stream().filter(descriptor -> evaluatePolicies(descriptor, agent))
                .map(descriptor -> new ContractDefinition(descriptor.getUsagePolicy(), descriptor.getSelectorExpression()));
    }

    /**
     * Loads {@link ContractDescriptor}s into the system.
     */
    public void load(Collection<ContractDescriptor> descriptors) {
        this.descriptors.addAll(descriptors);
    }

    /**
     * Removes descriptors matching the collection of ids.
     */
    public void remove(Collection<String> descriptorIds) {
        descriptorIds.forEach(id -> this.descriptors.removeIf(descriptor -> descriptor.getId().equals(id)));
    }

    /**
     * Returns the active descriptors.
     */
    public @NotNull List<ContractDescriptor> getLoadedDescriptors() {
        return descriptors;
    }

    /**
     * Determines the applicability of a descriptor to an agent by evaluating the union of its access control and usage policies.
     */
    private boolean evaluatePolicies(ContractDescriptor descriptor, ParticipantAgent agent) {
        var accessResult = policyEngine.evaluate(descriptor.getAccessControlPolicy(), agent);
        if (!accessResult.valid()) {
            monitor.info(format("Problem evaluating access control policy for %s: \n%s", descriptor.getId(), String.join("\n", accessResult.getProblems())));
            return false;
        }
        var usageResult = policyEngine.evaluate(descriptor.getUsagePolicy(), agent);
        if (!usageResult.valid()) {
            monitor.info(format("Problem evaluating usage control policy for %s: \n%s", descriptor.getId(), String.join("\n", accessResult.getProblems())));
            return false;
        }
        return true;
    }
}
