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

import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParticipantAgentContextMapperTest {

    private final ParticipantAgentContextMapper<ParticipantAgentPolicyContext> mapper = new ParticipantAgentContextMapper<>();

    @SuppressWarnings("unchecked")
    @Test
    void mapContext() {
        var ctx = mock(ParticipantAgentPolicyContext.class);
        var attributes = Map.of("key1", "value1", "key2", "value2");
        var participantAgent = new ParticipantAgent("agent-id", Map.of(), attributes);
        when(ctx.participantAgent()).thenReturn(participantAgent);
        var result = mapper.mapContext(ctx);

        assertThat(result).isSucceeded().satisfies(map -> {
            var agentMap = (Map<String, Object>) map.get("agent");
            Assertions.assertThat(agentMap.get("id")).isEqualTo("agent-id");
            Assertions.assertThat(agentMap.get("attributes")).isEqualTo(attributes);
            Assertions.assertThat(agentMap.get("claims")).isEqualTo(Map.of());
        });

    }

    @SuppressWarnings("unchecked")
    @Test
    void mapContext_withCredentials() {
        var ctx = mock(ParticipantAgentPolicyContext.class);
        var attributes = Map.of("key1", "value1", "key2", "value2");

        var credentials = credentials();
        Map<String, Object> claims = Map.of(
                "vc", credentials
        );
        var participantAgent = new ParticipantAgent("agent-id", claims, attributes);
        when(ctx.participantAgent()).thenReturn(participantAgent);
        var result = mapper.mapContext(ctx);

        assertThat(result).isSucceeded().satisfies(map -> {
            var agentMap = (Map<String, Object>) map.get("agent");

            var expectedMap = Map.of("vc", List.of(
                    Map.of(
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
            ));
            Assertions.assertThat(agentMap.get("claims")).isEqualTo(expectedMap);
        });
    }

    @SuppressWarnings("unchecked")
    private List<VerifiableCredential> credentials() {
        var vc = VerifiableCredential.Builder.newInstance()
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
