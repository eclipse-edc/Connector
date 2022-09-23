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
 *       Fraunhofer Institute for Software and Systems Engineering - add test
 *
 */

package org.eclipse.dataspaceconnector.transfer.provision.http.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpProviderResourceDefinitionTest {
    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var request = HttpProviderResourceDefinition.Builder.newInstance().assetId("123").transferProcessId("1").id("2").dataAddressType("test").build();

        var serialized = mapper.writeValueAsString(request);

        var deserialized = mapper.readValue(serialized, HttpProviderResourceDefinition.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getAssetId()).isEqualTo("123");
        assertThat(deserialized.getTransferProcessId()).isEqualTo("1");
        assertThat(deserialized.getDataAddressType()).isEqualTo("test");

    }
    
    @Test
    void toBuilder_verifyEqualResourceDefinition() {
        var definition = HttpProviderResourceDefinition.Builder.newInstance()
                .id("id")
                .transferProcessId("tp-id")
                .assetId("asset")
                .dataAddressType("type")
                .build();
        var builder = definition.toBuilder();
        var rebuiltDefinition = builder.build();
    
        assertThat(rebuiltDefinition).usingRecursiveComparison().isEqualTo(definition);
    }
}
