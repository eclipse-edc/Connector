/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.types.TypeManager;
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