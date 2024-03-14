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

package org.eclipse.edc.test.e2e;

import jakarta.json.JsonObject;
import org.eclipse.edc.test.e2e.participant.EndToEndTransferParticipant;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public abstract class TransferEndToEndTestBase {

    protected final Duration timeout = Duration.ofSeconds(60);

    protected static final EndToEndTransferParticipant CONSUMER = EndToEndTransferParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    protected static final EndToEndTransferParticipant PROVIDER = EndToEndTransferParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    protected void createResourcesOnProvider(String assetId, JsonObject policy, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var policyDefinition = PROVIDER.createPolicyDefinition(policy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), policyDefinition, policyDefinition);
    }
}
