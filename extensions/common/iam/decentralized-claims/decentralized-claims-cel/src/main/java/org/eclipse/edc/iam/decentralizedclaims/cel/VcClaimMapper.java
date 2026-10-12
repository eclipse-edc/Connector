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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.cel.function.context.CelClaim;
import org.eclipse.edc.policy.cel.function.context.CelParticipantAgentClaimMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                    .map(this::toCredentialMap)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> toCredentialMap(Object item) {
        if (item instanceof VerifiableCredential credential) {
            return toMap(credential);
        }
        // credentials read back from a persisted claims map (e.g. a SQL store) are deserialized as plain maps
        if (item instanceof Map<?, ?> credential) {
            return fromSerializedMap(credential);
        }
        return null;
    }

    private Map<String, Object> toMap(VerifiableCredential credential) {
        var cred = new HashMap<String, Object>();
        cred.put("@context", credential.getContext());
        cred.put("id", credential.getId());
        cred.put("type", credential.getType());
        cred.put("credentialSubject", credential.getCredentialSubject().stream().map(this::toSubjectMap).collect(Collectors.toList()));
        cred.put("issuer", toIssuerMap(credential.getIssuer()));
        cred.put("issuanceDate", credential.getIssuanceDate().toString());
        // optional: only emitted when present, so expressions must not assume it exists
        if (credential.getExpirationDate() != null) {
            cred.put("expirationDate", credential.getExpirationDate().toString());
        }
        return cred;
    }

    /**
     * Normalizes a {@link VerifiableCredential} serialized as JSON and deserialized as a map to the same shape produced by
     * {@link #toMap(VerifiableCredential)}.
     */
    private Map<String, Object> fromSerializedMap(Map<?, ?> credential) {
        var cred = new HashMap<String, Object>();
        cred.put("@context", credential.get("@context") instanceof List<?> context ? context : List.of());
        cred.put("id", credential.get("id"));
        cred.put("type", credential.get("type") instanceof List<?> type ? type : List.of());
        cred.put("credentialSubject", credential.get("credentialSubject") instanceof List<?> subjects ? subjects : List.of());
        cred.put("issuer", toSerializedIssuerMap(credential.get("issuer")));
        cred.put("issuanceDate", toDateString(credential.get("issuanceDate")));
        var expirationDate = toDateString(credential.get("expirationDate"));
        if (expirationDate != null) {
            cred.put("expirationDate", expirationDate);
        }
        return cred;
    }

    private Object toSerializedIssuerMap(Object issuer) {
        if (issuer instanceof String id) {
            return Map.of("id", id);
        }
        if (issuer instanceof Map<?, ?> issuerMap) {
            // an Issuer is serialized as {"id": ..., "additionalProperties": {...}}
            var map = new HashMap<Object, Object>();
            if (issuerMap.get("additionalProperties") instanceof Map<?, ?> additionalProperties) {
                map.putAll(additionalProperties);
            }
            issuerMap.forEach((key, value) -> {
                if (!"additionalProperties".equals(key)) {
                    map.put(key, value);
                }
            });
            return map;
        }
        return issuer;
    }

    private String toDateString(Object date) {
        if (date instanceof String string) {
            return string;
        }
        // instants serialized as timestamps are expressed in seconds, with an optional fraction of nanoseconds
        if (date instanceof Number number) {
            var seconds = new BigDecimal(number.toString());
            return Instant.ofEpochSecond(seconds.longValue(), seconds.remainder(BigDecimal.ONE).movePointRight(9).longValue()).toString();
        }
        return null;
    }

    private Map<String, Object> toSubjectMap(CredentialSubject subject) {
        var claims = subject.getClaims();
        // an explicit "id" claim takes precedence over the subject id; avoid copying when there is nothing to add
        if (subject.getId() == null || claims.containsKey("id")) {
            return claims;
        }
        var merged = new HashMap<String, Object>(claims);
        merged.put("id", subject.getId());
        return merged;
    }

    private Map<String, Object> toIssuerMap(Issuer issuer) {
        if (issuer.additionalProperties().isEmpty()) {
            return Map.of("id", issuer.id());
        }
        var map = new HashMap<String, Object>(issuer.additionalProperties());
        map.put("id", issuer.id());
        return map;
    }

}
