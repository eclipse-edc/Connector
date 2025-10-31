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
import org.eclipse.edc.junit.annotations.Runtime;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@SuppressWarnings("JUnitMalformedDeclaration")
public abstract class TransferEndToEndTestBase {

    public static final String PROVIDER_DP = "provider-data-plane";
    public static final String CONSUMER_DP = "consumer-data-plane";
    public static final String PROVIDER_CP = "provider-control-plane";
    public static final String CONSUMER_CP = "consumer-control-plane";
    public static final String CONSUMER_ID = "urn:connector:consumer";
    public static final String PROVIDER_ID = "urn:connector:provider";

    protected static String privateKey = getResourceFileContentAsString("certs/key.pem");
    protected static String publicKey = getResourceFileContentAsString("certs/cert.pem");

    protected static String noConstraintPolicyId;
    protected final Duration timeout = Duration.ofSeconds(60);

    @BeforeAll
    static void setup(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
        noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
    }

    protected void createResourcesOnProvider(TransferEndToEndParticipant provider, String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var contractPolicyId = provider.createPolicyDefinition(contractPolicy);
        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, contractPolicyId);
    }

    protected void createResourcesOnProvider(TransferEndToEndParticipant provider, String assetId, Map<String, Object> dataAddressProperties) {
        provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
    }
}
