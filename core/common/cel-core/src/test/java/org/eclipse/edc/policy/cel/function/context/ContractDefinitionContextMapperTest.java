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

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ContractDefinitionContextMapperTest {

    private final ContractDefinitionContextMapper mapper = new ContractDefinitionContextMapper();
    private final ParticipantAgent agent = new ParticipantAgent("agent", Map.of(), Map.of());

    @Test
    void mapContext_shouldExposeContractDefinition() {
        var definition = ContractDefinition.Builder.newInstance()
                .id("definition-id")
                .participantContextId("pc")
                .accessPolicyId("access-policy")
                .contractPolicyId("contract-policy")
                .privateProperty("private-key", "private-value")
                .assetsSelector(List.of(Criterion.criterion("id", "=", "asset-id")))
                .build();
        var context = new ApprovalContractNegotiationPolicyContext("pc", agent, null, definition);

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).containsOnlyKeys("contractDefinition")
                .extractingByKey("contractDefinition").asInstanceOf(MAP)
                .containsEntry("id", "definition-id")
                .containsEntry("participantContextId", "pc")
                .containsEntry("accessPolicyId", "access-policy")
                .containsEntry("contractPolicyId", "contract-policy")
                .containsEntry("privateProperties", Map.of("private-key", "private-value"))
                .containsEntry("assetsSelector", List.of(Map.of("operandLeft", "id", "operator", "=", "operandRight", "asset-id"))));
    }

    @Test
    void mapContext_shouldBeEmpty_whenNoContractDefinition() {
        var context = new ApprovalContractNegotiationPolicyContext("pc", agent, null, null);

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).isEmpty());
    }

    @Test
    void mapContext_shouldBeEmpty_whenContextHasNoContractDefinition() {
        var context = new ContractNegotiationPolicyContext("pc", agent);

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).isEmpty());
    }
}
