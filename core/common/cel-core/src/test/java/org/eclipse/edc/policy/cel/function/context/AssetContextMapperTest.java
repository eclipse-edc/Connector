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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class AssetContextMapperTest {

    private final AssetContextMapper mapper = new AssetContextMapper();
    private final ParticipantAgent agent = new ParticipantAgent("agent", Map.of(), Map.of());

    @Test
    void mapContext_shouldExposeAsset() {
        var asset = Asset.Builder.newInstance()
                .id("asset-id")
                .participantContextId("pc")
                .property("key", "value")
                .privateProperty("private-key", "private-value")
                .build();
        var context = new ApprovalContractNegotiationPolicyContext("pc", agent, asset, null);

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).containsOnlyKeys("asset")
                .extractingByKey("asset").asInstanceOf(MAP)
                .containsEntry("id", "asset-id")
                .containsEntry("participantContextId", "pc")
                .containsEntry("privateProperties", Map.of("private-key", "private-value"))
                .hasEntrySatisfying("properties", properties -> assertThat(properties).asInstanceOf(MAP).containsEntry("key", "value")));
    }

    @Test
    void mapContext_shouldBeEmpty_whenNoAsset() {
        var context = new ApprovalContractNegotiationPolicyContext("pc", agent, null, null);

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).isEmpty());
    }

    @Test
    void mapContext_shouldBeEmpty_whenContextHasNoAsset() {
        var context = new ContractNegotiationPolicyContext("pc", agent);

        var result = mapper.mapContext(context);

        assertThat(result).isSucceeded().satisfies(map -> assertThat(map).isEmpty());
    }
}
