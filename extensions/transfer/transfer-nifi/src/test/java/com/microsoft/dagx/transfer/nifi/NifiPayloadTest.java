package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


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
                .key("mykey")
                .type("AzureStorage")
                .property("blobname", "testblob")
                .property("container", "testcontainer").build();
        var nifipayload = new NifiPayload(endpoint, endpoint);

        var json = typeManager.writeValueAsString(nifipayload);

        assertThat(json).isNotEmpty();
        assertThat(json).contains("testaccount")
                .contains("mykey")
                .contains("testblob")
                .contains("testcontainer")
                .contains("source")
                .contains("destination")
                .contains("AzureStorage");
    }

    @Test
    void deserialize() {
        NifiTransferEndpoint endpoint = NifiTransferEndpoint.NifiTransferEndpointBuilder.newInstance()
                .type("AzureStorage")
                .key("mykey")
                .property("account", "testaccount")
                .property("blobname", "testblob")
                .property("container", "testcontainer").build();
        var nifipayload = new NifiPayload(endpoint, endpoint);

        var json = typeManager.writeValueAsString(nifipayload);

        var deserialized = typeManager.readValue(json, NifiPayload.class);
        assertThat(deserialized.getSource().getKey()).isEqualTo("mykey");
        assertThat(deserialized.getSource().getType()).isEqualTo("AzureStorage");
        assertThat(deserialized.getSource().getProperties())
                .hasSize(3)
                .containsEntry("blobname", "testblob")
                .containsEntry("container", "testcontainer")
                .containsEntry("account", "testaccount")
                .doesNotContainValue(null);

        assertThat(deserialized.getDestination().getKey()).isEqualTo("mykey");
        assertThat(deserialized.getDestination().getType()).isEqualTo("AzureStorage");
        assertThat(deserialized.getDestination().getProperties())
                .hasSize(3)
                .containsEntry("blobname", "testblob")
                .containsEntry("container", "testcontainer")
                .containsEntry("account", "testaccount")
                .doesNotContainValue(null);

    }

}