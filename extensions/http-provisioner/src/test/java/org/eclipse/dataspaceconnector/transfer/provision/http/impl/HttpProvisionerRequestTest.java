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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpProvisionerRequestTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var request = HttpProvisionerRequest.Builder.newInstance().assetId("123").transferProcessId("1").callbackAddress("http://test.com").build();

        var serialized = mapper.writeValueAsString(request);

        var deserialized = mapper.readValue(serialized, HttpProvisionerRequest.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getAssetId()).isEqualTo("123");
        assertThat(deserialized.getTransferProcessId()).isEqualTo("1");
        assertThat(deserialized.getCallbackAddress()).isEqualTo("http://test.com");

    }
}
