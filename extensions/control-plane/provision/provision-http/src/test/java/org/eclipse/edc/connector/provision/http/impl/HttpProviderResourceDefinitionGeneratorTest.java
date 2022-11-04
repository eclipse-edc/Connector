/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.provision.http.impl;


import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HttpProviderResourceDefinitionGeneratorTest {
    private static final String DATA_ADDRESS_TYPE = "test-address";

    private HttpProviderResourceDefinitionGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new HttpProviderResourceDefinitionGenerator(DATA_ADDRESS_TYPE);
    }


    @Test
    void verifyGeneration() {
        var dataRequest = DataRequest.Builder.newInstance().destinationType("destination").assetId("asset-id").processId("process-id").build();
        var policy = Policy.Builder.newInstance().build();

        var assetAddress1 = DataAddress.Builder.newInstance().type(DATA_ADDRESS_TYPE).build();

        var definition = generator.generate(dataRequest, assetAddress1, policy);

        assertThat(definition).isInstanceOf(HttpProviderResourceDefinition.class);
        var objectDef = (HttpProviderResourceDefinition) definition;
        assertThat(objectDef.dataAddressType).isEqualTo("test-address");
        assertThat(objectDef.getTransferProcessId()).isEqualTo("process-id");
        assertThat(objectDef.getAssetId()).isEqualTo("asset-id");
    }

    @Test
    void canGenerate() {

        var dataRequest = DataRequest.Builder.newInstance().destinationType("destination").assetId("asset-id").processId("process-id").build();
        var policy = Policy.Builder.newInstance().build();

        var assetAddress1 = DataAddress.Builder.newInstance().type(DATA_ADDRESS_TYPE).build();

        var definition = generator.canGenerate(dataRequest, assetAddress1, policy);
        assertThat(definition).isTrue();
    }

    @Test
    void canGenerate_dataAddressTypeDifferentThanAssetAddressType() {

        var dataRequest = DataRequest.Builder.newInstance().destinationType("destination").assetId("asset-id").processId("process-id").build();
        var policy = Policy.Builder.newInstance().build();

        var assetAddress1 = DataAddress.Builder.newInstance().type("a-different-addressType").build();

        var definition = generator.canGenerate(dataRequest, assetAddress1, policy);
        assertThat(definition).isFalse();
    }

}
