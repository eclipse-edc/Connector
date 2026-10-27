/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.ContractDefinitionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.ResolvedContractDefinitions;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toCollection;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.IN;

/**
 * Determines the contract definitions applicable to a {@link ParticipantAgent} by evaluating the access control and
 * usage policies associated with a set of assets as defined by {@link ContractDefinition}s. On the distinction between
 * access control and usage policy, see {@link ContractDefinition}.
 */
public class ContractDefinitionResolverImpl implements ContractDefinitionResolver {

    /**
     * How many policies are loaded with a single query, to keep the number of query parameters bounded.
     */
    static final int POLICY_BATCH_SIZE = 1000;

    private final PolicyEngine policyEngine;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore definitionStore;

    public ContractDefinitionResolverImpl(ContractDefinitionStore contractDefinitionStore, PolicyEngine policyEngine, PolicyDefinitionStore policyStore) {
        definitionStore = contractDefinitionStore;
        this.policyEngine = policyEngine;
        this.policyStore = policyStore;
    }

    @Override
    public ResolvedContractDefinitions resolveFor(ParticipantContext participantContext, ParticipantAgent agent) {
        var query = queryByParticipantContextId(participantContext.getId()).limit(Integer.MAX_VALUE).build();
        var definitions = definitionStore.findAll(query).toList();

        var accessPolicyIds = definitions.stream().map(ContractDefinition::getAccessPolicyId).collect(toCollection(LinkedHashSet::new));
        var policies = loadPolicies(participantContext.getId(), accessPolicyIds);

        // the evaluation does not depend on the contract definition, so every access policy is evaluated only once
        var accessGranted = new HashMap<String, Boolean>();
        var granted = definitions.stream()
                .filter(definition -> accessGranted.computeIfAbsent(definition.getAccessPolicyId(), policyId -> {
                    var policy = policies.get(policyId);
                    return policy != null && policyEngine.evaluate(policy, new CatalogPolicyContext(participantContext.getId(), agent)).succeeded();
                }))
                .toList();

        return new ResolvedContractDefinitions(granted, policies);
    }

    private Map<String, Policy> loadPolicies(String participantContextId, LinkedHashSet<String> policyIds) {
        var policies = new HashMap<String, Policy>();
        var ids = new ArrayList<>(policyIds);
        for (var from = 0; from < ids.size(); from += POLICY_BATCH_SIZE) {
            var batch = List.copyOf(ids.subList(from, Math.min(from + POLICY_BATCH_SIZE, ids.size())));
            var query = queryByParticipantContextId(participantContextId)
                    .filter(criterion("id", IN, batch))
                    .limit(batch.size())
                    .build();
            try (var found = policyStore.findAll(query)) {
                found.forEach(definition -> policies.put(definition.getId(), definition.getPolicy()));
            }
        }
        return policies;
    }

}
