/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.cel;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.cel.function.context.CelClaim;
import org.eclipse.edc.policy.cel.function.context.CelParticipantAgentClaimMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VcClaimMapper implements CelParticipantAgentClaimMapper {

    public static final String VC_CLAIM = "vc";

    // Converts the 'vc' claim to a list of verifiable credential maps.
    @Override
    public CelClaim mapClaim(ParticipantAgent agent) {
        var vcClaim = agent.getClaims().get(VC_CLAIM);
        if (vcClaim == null) {
            return new CelClaim(VC_CLAIM, null);
        }
        return new CelClaim(VC_CLAIM, toVcList(vcClaim));
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
        cred.put("@context", credential.getContext());
        cred.put("id", credential.getId());
        cred.put("type", credential.getType());
        cred.put("credentialSubject", credential.getCredentialSubject().stream().map(CredentialSubject::getClaims).collect(Collectors.toList()));
        cred.put("issuer", credential.getIssuer().id());
        cred.put("issuanceDate", credential.getIssuanceDate().toString());
        return cred;
    }

}
