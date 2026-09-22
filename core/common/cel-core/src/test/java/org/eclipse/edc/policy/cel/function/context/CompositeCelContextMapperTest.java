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

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class CompositeCelContextMapperTest {

    private final CatalogPolicyContext context = new CatalogPolicyContext("pc", new ParticipantAgent("agent", Map.of(), Map.of()));

    @Test
    void mapContext_shouldMergeInOrder_laterWins() {
        CelContextMapper<CatalogPolicyContext> first = c -> Result.success(Map.of("a", 1, "shared", "first"));
        CelContextMapper<CatalogPolicyContext> second = c -> Result.success(Map.of("b", 2, "shared", "second"));

        var result = CompositeCelContextMapper.of(first, second).mapContext(context);

        assertThat(result).isSucceeded().satisfies(map ->
                assertThat(map).containsEntry("a", 1).containsEntry("b", 2).containsEntry("shared", "second"));
    }

    @Test
    void mapContext_shouldFail_whenAnyMapperFails() {
        CelContextMapper<CatalogPolicyContext> ok = c -> Result.success(Map.of("a", 1));
        CelContextMapper<CatalogPolicyContext> failing = c -> Result.failure("boom");

        var result = CompositeCelContextMapper.of(ok, failing).mapContext(context);

        assertThat(result).isFailed().detail().isEqualTo("boom");
    }

    @Test
    void mapContext_shouldCombineAgentAndPartnersHandle() {
        var result = CompositeCelContextMapper.<CatalogPolicyContext>of(
                new ParticipantAgentContextMapper<>(new CelParticipantAgentClaimMapperRegistryImpl()),
                new ParticipantContextContextMapper<>()).mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).containsKeys("agent", "partners"));
    }
}
