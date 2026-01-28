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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Supplies participant agent-related context data for CEL expression evaluation.
 *
 * @param <C> the type of ParticipantAgentPolicyContext
 */
public class ParticipantAgentContextMapper<C extends ParticipantAgentPolicyContext> implements CelContextMapper<C> {

    private Result<Map<String, Object>> agent(C context) {
        if (context.participantAgent() == null) {
            return Result.failure("Participant agent is null");
        }
        var id = context.participantAgent().getIdentity();
        return Result.success(Map.of("agent", Map.ofEntries(
                Map.entry("id", id),
                Map.entry("attributes", context.participantAgent().getAttributes()),
                Map.entry("claims", toClaimsMap(context.participantAgent().getClaims()))
        )));
    }

    // Converts the 'vc' claim to a list of verifiable credential maps.
    // TODO Currently, hardcoded here; in the future, consider using a more generic approach.
    private Map<String, Object> toClaimsMap(Map<String, Object> claims) {
        var mappedClaims = new HashMap<>(claims);
        if (claims.get("vc") == null) {
            return mappedClaims;
        }
        mappedClaims.put("vc", toVcList(claims.get("vc")));
        return mappedClaims;
    }

    private List<Map<String, Object>> toVcList(Object vcClaim) {
        if (vcClaim instanceof List<?> vcList) {
            return vcList.stream()
                    .filter(item -> item instanceof VerifiableCredential)
                    .map(item -> toMap((VerifiableCredential) item))
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> toMap(VerifiableCredential credential) {
        var cred = new HashMap<String, Object>();
        cred.put("id", credential.getId());
        cred.put("type", credential.getType());
        cred.put("credentialSubject", credential.getCredentialSubject().stream().map(this::toMap).collect(Collectors.toList()));
        cred.put("issuer", credential.getIssuer().id());
        cred.put("issuanceDate", credential.getIssuanceDate().toString());
        return cred;
    }

    private Map<String, Object> toMap(CredentialSubject subject) {
        return subject.getClaims();
    }

    @Override
    public Result<Map<String, Object>> mapContext(C context) {
        return agent(context);
    }
}
