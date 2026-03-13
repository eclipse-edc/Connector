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

package org.eclipse.edc.iam.decentralizedclaims.issuer.configuration;

import org.eclipse.edc.iam.decentralizedclaims.cel.VcClaimMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.cel.VcClaimMapper.VC_CLAIM;

public class VeClaimMapperTest {

    private final VcClaimMapper mapper = new VcClaimMapper();

    @Test
    void mapClaim() {
        var attributes = Map.of("key1", "value1", "key2", "value2");

        var credentials = credentials();
        Map<String, Object> claims = Map.of(
                "vc", credentials
        );
        var participantAgent = new ParticipantAgent("agent-id", claims, attributes);
        var result = mapper.mapClaim(participantAgent);

        var expectedList = List.of(
                Map.of(
                        "@context", List.of("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
                        "id", "http://example.edu/credentials/3732",
                        "type", List.of("VerifiableCredential", "AlumniCredential"),
                        "issuer", "https://example.edu/issuers/14",
                        "issuanceDate", credentials.get(0).getIssuanceDate().toString(),
                        "credentialSubject", List.of(Map.of(
                                "alumniOf", "Example University",
                                "degree", Map.of(
                                        "type", "BachelorDegree",
                                        "name", "Bachelor of Science and Arts"
                                )
                        ))
                )
        );

        assertThat(result.name()).isEqualTo(VC_CLAIM);
        assertThat(result.value()).isEqualTo(expectedList);
    }

    @Test
    void mapClaim_vcNotFound() {
        var participantAgent = new ParticipantAgent("agent-id", Map.of(), Map.of());
        var result = mapper.mapClaim(participantAgent);

        assertThat(result.name()).isEqualTo(VC_CLAIM);
        assertThat(result.value()).isNull();
    }

    @SuppressWarnings("unchecked")
    private List<VerifiableCredential> credentials() {
        var vc = VerifiableCredential.Builder.newInstance()
                .contexts(List.of("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"))
                .id("http://example.edu/credentials/3732")
                .types(List.of("VerifiableCredential", "AlumniCredential"))
                .issuer(new Issuer("https://example.edu/issuers/14", Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .claim("alumniOf", "Example University")
                        .claim("degree", Map.of(
                                "type", "BachelorDegree",
                                "name", "Bachelor of Science and Arts"
                        ))
                        .build())
                .build();
        return List.of(vc);
    }
}
