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

package org.eclipse.edc.tck.dsp.data;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Seed data for the TCK scenarios.
 */
public class DataSeed {
    private static final Set<String> ASSET_IDS = Set.of("ACN0101", "ACN0102", "ACN0103", "ACN0104",
            "ACN0201", "ACN0202", "ACN0203", "ACN0204", "ACN0205", "ACN0206", "ACN0207",
            "ACN0301", "ACN0302", "ACN0303", "ACN0304",
            "CAT0101", "CAT0102");
    private static final Set<String> AGREEMENT_IDS = Set.of(
            "ATP0101", "ATP0102", "ATP0103", "ATP0104", "ATP0105",
            "ATP0201", "ATP0202", "ATP0203", "ATP0204", "ATP0205",
            "ATP0301", "ATP0302", "ATP0303", "ATP0304", "ATP0305", "ATP0306",
            "ATPC0101", "ATPC0102", "ATPC0103", "ATPC0104", "ATPC0105",
            "ATPC0201", "ATPC0202", "ATPC0203", "ATPC0204", "ATPC0205",
            "ATPC0301", "ATPC0302", "ATPC0303", "ATPC0304", "ATPC0305", "ATPC0306");
    private static final String POLICY_ID = "P123";
    private static final String CONTRACT_DEFINITION_ID = "CD123";

    private DataSeed() {
    }

    public static Set<Asset> createAssets(String participantContextId) {
        var assets = ASSET_IDS.stream().map(id -> createAsset(id, participantContextId)).collect(toSet());

        assets.add(Asset.Builder.newInstance().id("ATP0101")
                .dataAddress(DataAddress.Builder.newInstance().type("HttpData").build())
                .participantContextId(participantContextId)
                .build());
        return assets;
    }

    public static Set<PolicyDefinition> createPolicyDefinitions(String participantContextId) {
        var permission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("http://www.w3.org/ns/odrl/2/use").build())
                .build();
        return Set.of(PolicyDefinition.Builder.newInstance()
                .id(POLICY_ID)
                .policy(Policy.Builder.newInstance().permission(permission).build())
                .participantContextId(participantContextId)
                .build());
    }

    public static Set<ContractDefinition> createContractDefinitions(String participantContextId) {
        return Set.of(ContractDefinition.Builder.newInstance()
                .id(CONTRACT_DEFINITION_ID)
                .accessPolicyId(POLICY_ID)
                .contractPolicyId(POLICY_ID)
                .participantContextId(participantContextId)
                .build());
    }


    public static Set<ContractNegotiation> createContractNegotiations(String participantContextId) {
        return AGREEMENT_IDS.stream().map(id -> createContractNegotiation(id, participantContextId)).collect(toSet());
    }

    private static Asset createAsset(String id, String participantContextId) {
        return Asset.Builder.newInstance()
                .id(id)
                .dataAddress(DataAddress.Builder.newInstance().type("HttpData").build())
                .participantContextId(participantContextId)
                .build();
    }

    private static ContractNegotiation createContractNegotiation(String id, String participantContextId) {

        return ContractNegotiation.Builder.newInstance()
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .id(id)
                        .providerId("providerId")
                        .consumerId("TCK_PARTICIPANT")
                        .assetId("ATP0101")
                        .contractSigningDate(System.currentTimeMillis())
                        .policy(Policy.Builder.newInstance().build())
                        .participantContextId(participantContextId)
                        .agreementId(id)
                        .build())
                .type(ContractNegotiation.Type.PROVIDER)
                .state(ContractNegotiationStates.FINALIZED.code())
                .counterPartyId("counterPartyId")
                .counterPartyAddress("https://test.com")
                .protocol("test")
                .participantContextId(participantContextId)
                .build();
    }
}
