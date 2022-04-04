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
package org.eclipse.dataspaceconnector.transfer.provision.http.impl;


import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpProviderResourceDefinitionGeneratorTest {
    private static final String DATA_ADDRESS_TYPE = "test-address";

    private HttpProviderResourceDefinitionGenerator generator;

    @Test
    void verifyGeneration() {
        var dataRequest = DataRequest.Builder.newInstance().destinationType("destination").assetId("id").build();
        var policy = Policy.Builder.newInstance().build();

        var assetAddress1 = DataAddress.Builder.newInstance().type(DATA_ADDRESS_TYPE).build();
        assertThat(generator.generate(dataRequest, assetAddress1, policy)).isNotNull();

        var assetAddress2 = DataAddress.Builder.newInstance().type("another-type").build();
        assertThat(generator.generate(dataRequest, assetAddress2, policy)).isNull();
    }


    @BeforeEach
    void setUp() {
        generator = new HttpProviderResourceDefinitionGenerator(DATA_ADDRESS_TYPE);
    }
}
