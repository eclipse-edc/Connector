/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.provision.http.webhook;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProvisionerWebhookRequestSerializationTest {

    private TypeManager typeManager;

    @BeforeEach
    void setUp() {
        typeManager = new TypeManager();
        typeManager.registerTypes(ProvisionerWebhookRequest.class);
    }

    @Test
    void verifySerialization() {
        var rq = ProvisionerWebhookRequest.Builder.newInstance()
                .assetId("test-asset")
                .resourceName("test-resource")
                .resourceDefinitionId("test-res-def")
                .hasToken(true)
                .apiToken("barbaz")
                .contentDataAddress(DataAddress.Builder.newInstance().type("test-type").build())
                .build();
        var json = typeManager.writeValueAsString(rq);

        assertThat(json).contains("test-asset")
                .contains("test-resource")
                .contains("test-type")
                .contains("hasToken")
                .contains("dataspaceconnector:provisioner-callback-request");
    }

    @Test
    void verifyDeserialization() {
        var rq = ProvisionerWebhookRequest.Builder.newInstance()
                .assetId("test-asset")
                .resourceName("test-resource")
                .resourceDefinitionId("test-res-def")
                .apiToken("foobar")
                .hasToken(true)
                .contentDataAddress(DataAddress.Builder.newInstance().type("test-type").build())
                .build();
        var json = typeManager.writeValueAsString(rq);

        var deserialized = typeManager.readValue(json, ProvisionerWebhookRequest.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(rq);
    }

}