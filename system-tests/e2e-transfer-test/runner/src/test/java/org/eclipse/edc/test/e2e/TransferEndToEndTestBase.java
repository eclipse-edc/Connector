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

import org.eclipse.edc.junit.annotations.Runtime;
import org.junit.jupiter.api.BeforeAll;

import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;

@SuppressWarnings("JUnitMalformedDeclaration")
public abstract class TransferEndToEndTestBase {

    public static final String PROVIDER_DP = "provider-data-plane";
    public static final String CONSUMER_DP = "consumer-data-plane";
    public static final String PROVIDER_CP = "provider-control-plane";
    public static final String CONSUMER_CP = "consumer-control-plane";
    public static final String CONSUMER_ID = "urn:connector:consumer";
    public static final String PROVIDER_ID = "urn:connector:provider";

    protected static String noConstraintPolicyId;

    @BeforeAll
    static void setup(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {
        noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
    }

}
