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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParticipantAgentContextMapperTest {

    private final CelParticipantAgentClaimMapperRegistry claimMapperRegistry = mock();
    private final ParticipantAgentContextMapper<ParticipantAgentPolicyContext> mapper = new ParticipantAgentContextMapper<>(claimMapperRegistry);

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
            assertThat(agentMap.get("id")).isEqualTo("agent-id");
            assertThat(agentMap.get("attributes")).isEqualTo(attributes);
            assertThat(agentMap.get("claims")).isEqualTo(Map.of());
        });

    }

    @SuppressWarnings("unchecked")
    @Test
    void mapContext_claimsMapper() {
        var ctx = mock(ParticipantAgentPolicyContext.class);
        when(claimMapperRegistry.mapClaim(any())).thenReturn(List.of(
                new CelClaim("claim1", "value1"),
                new CelClaim("claim2", "value2")
        ));
        var attributes = Map.of("key1", "value1", "key2", "value2");

        var participantAgent = new ParticipantAgent("agent-id", Map.of(), attributes);
        when(ctx.participantAgent()).thenReturn(participantAgent);
        var result = mapper.mapContext(ctx);

        assertThat(result).isSucceeded().satisfies(map -> {
            var agentMap = (Map<String, Object>) map.get("agent");
            assertThat(agentMap.get("id")).isEqualTo("agent-id");
            assertThat(agentMap.get("attributes")).isEqualTo(attributes);
            assertThat(agentMap.get("claims")).isEqualTo(Map.of(
                    "claim1", "value1",
                    "claim2", "value2"
            ));
        });
    }
}
