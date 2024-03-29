/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.signaling;

import jakarta.json.JsonObject;
import org.eclipse.edc.test.e2e.participant.SignalingParticipant;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;

public abstract class SignalingEndToEndTestBase {

    protected static final SignalingParticipant CONSUMER = SignalingParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    protected static final SignalingParticipant PROVIDER = SignalingParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    protected void createResourcesOnProvider(String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var accessPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), accessPolicyId, contractPolicyId);
    }

    protected void registerDataPlanes() {
        PROVIDER.registerDataPlane(Set.of("HttpData-PUSH", "HttpData-PULL"));
    }

}
