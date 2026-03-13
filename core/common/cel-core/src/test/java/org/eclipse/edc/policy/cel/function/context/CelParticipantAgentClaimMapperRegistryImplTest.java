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

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CelParticipantAgentClaimMapperRegistryImplTest {

    @Test
    void mapClaim_emptyRegistry_returnsEmptyList() {
        var registry = new CelParticipantAgentClaimMapperRegistryImpl();
        var agent = new ParticipantAgent("agent-id", Map.of(), Map.of());

        var result = registry.mapClaim(agent);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void registerClaimMapper_mapsClaimsInRegistrationOrder() {
        var registry = new CelParticipantAgentClaimMapperRegistryImpl();
        var agent = new ParticipantAgent("agent-id", Map.of(), Map.of());

        registry.registerClaimMapper(a -> new CelClaim("claim1", "value1"));
        registry.registerClaimMapper(a -> new CelClaim("claim2", "value2"));

        List<CelClaim> result = registry.mapClaim(agent);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("claim1");
        assertThat(result.get(0).value()).isEqualTo("value1");
        assertThat(result.get(1).name()).isEqualTo("claim2");
        assertThat(result.get(1).value()).isEqualTo("value2");
    }

    @Test
    void mapClaim_includesClaimsWithNullValue() {
        var registry = new CelParticipantAgentClaimMapperRegistryImpl();
        var agent = new ParticipantAgent("agent-id", Map.of(), Map.of());

        registry.registerClaimMapper(a -> new CelClaim("nullable", null));

        var result = registry.mapClaim(agent);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("nullable");
        assertThat(result.get(0).value()).isNull();
    }
}

