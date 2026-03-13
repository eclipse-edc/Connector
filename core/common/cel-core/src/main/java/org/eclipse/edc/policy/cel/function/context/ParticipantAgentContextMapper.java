/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function.context;


import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * Supplies participant agent-related context data for CEL expression evaluation.
 *
 * @param <C> the type of ParticipantAgentPolicyContext
 */
public class ParticipantAgentContextMapper<C extends ParticipantAgentPolicyContext> implements CelContextMapper<C> {

    private final CelParticipantAgentClaimMapperRegistry claimMapperRegistry;

    public ParticipantAgentContextMapper(CelParticipantAgentClaimMapperRegistry claimMapperRegistry) {
        this.claimMapperRegistry = claimMapperRegistry;
    }

    private Result<Map<String, Object>> agent(C context) {
        if (context.participantAgent() == null) {
            return Result.failure("Participant agent is null");
        }
        var id = context.participantAgent().getIdentity();
        return Result.success(Map.of("agent", Map.ofEntries(
                Map.entry("id", id),
                Map.entry("attributes", context.participantAgent().getAttributes()),
                Map.entry("claims", toClaimsMap(context.participantAgent()))
        )));
    }


    private Map<String, Object> toClaimsMap(ParticipantAgent agent) {
        var mappedClaims = new HashMap<>(agent.getClaims());

        claimMapperRegistry.mapClaim(agent)
                .stream()
                .filter(claim -> claim.value() != null)
                .forEach(claim -> mappedClaims.put(claim.name(), claim.value()));

        return mappedClaims;
    }


    @Override
    public Result<Map<String, Object>> mapContext(C context) {
        return agent(context);
    }
}
