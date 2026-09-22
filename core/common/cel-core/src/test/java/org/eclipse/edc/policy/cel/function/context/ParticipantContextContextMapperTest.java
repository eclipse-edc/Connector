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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ParticipantContextContextMapperTest {

    private final ParticipantContextContextMapper<CatalogPolicyContext> mapper = new ParticipantContextContextMapper<>();

    @Test
    void mapContext_shouldExposePartnersHandle() {
        var context = new CatalogPolicyContext("pc", new ParticipantAgent("agent", Map.of(), Map.of()));

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map ->
                assertThat(map).containsEntry("partners", Map.of("participantContextId", "pc")));
    }

    @Test
    @SuppressWarnings("deprecation")
    void mapContext_shouldBeEmpty_whenNoParticipantContext() {
        var context = new CatalogPolicyContext(new ParticipantAgent("agent", Map.of(), Map.of()));

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).isEmpty());
    }
}
