/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.nifi;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NifiPayloadTest {

    private TypeManager typeManager;

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(NifiTransferEndpoint.class, NifiPayload.class);
    }

    @Test
    void serialize() {
        NifiTransferEndpoint endpoint = NifiTransferEndpoint.NifiTransferEndpointBuilder.newInstance()
                .property("account", "testaccount")
                .type("AzureStorage")
                .property("blobname", "testblob")
                .property("sas", "mykey")
                .property("container", "testcontainer").build();
        var id = UUID.randomUUID().toString();
        var nifipayload = new NifiPayload(id, endpoint, endpoint);

        var json = typeManager.writeValueAsString(nifipayload);

        assertThat(json).isNotEmpty();
        assertThat(json).contains("testaccount")
                .contains("mykey")
                .contains("testblob")
                .contains("testcontainer")
                .contains("source")
                .contains("destination")
                .contains("AzureStorage")
                .contains("\"requestId\":\"" + id + "\"");
    }

    @Test
    void deserialize() {
        NifiTransferEndpoint endpoint = NifiTransferEndpoint.NifiTransferEndpointBuilder.newInstance()
                .type("AzureStorage")
                .property("account", "testaccount")
                .property("blobname", "testblob")
                .property("sas", "mykey")
                .property("container", "testcontainer").build();
        var id = UUID.randomUUID().toString();
        var nifipayload = new NifiPayload(id, endpoint, endpoint);

        var json = typeManager.writeValueAsString(nifipayload);

        var deserialized = typeManager.readValue(json, NifiPayload.class);
        assertThat(deserialized.getSource().getType()).isEqualTo("AzureStorage");
        assertThat(deserialized.getSource().getProperties())
                .hasSize(4)
                .containsEntry("blobname", "testblob")
                .containsEntry("container", "testcontainer")
                .containsEntry("account", "testaccount")
                .containsEntry("sas", "mykey")
                .doesNotContainValue(null);

        assertThat(deserialized.getDestination().getType()).isEqualTo("AzureStorage");
        assertThat(deserialized.getDestination().getProperties())
                .hasSize(4)
                .containsEntry("blobname", "testblob")
                .containsEntry("container", "testcontainer")
                .containsEntry("account", "testaccount")
                .containsEntry("sas", "mykey")
                .doesNotContainValue(null);

    }

}
