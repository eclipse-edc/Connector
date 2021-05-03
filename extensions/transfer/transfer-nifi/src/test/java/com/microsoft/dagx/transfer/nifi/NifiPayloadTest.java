package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.transfer.nifi.azureblob.AzureTransferEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class NifiPayloadTest {

    private TypeManager typeManager;

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(NifiTransferEndpoint.class, AzureTransferEndpoint.class);
    }

    @Test
    void serialize() throws IOException {
        AzureTransferEndpoint endpoint = AzureTransferEndpoint.Builder.anAzureTransferEndpoint()
                .withAccount("testaccount")
                .withKey("mykey")
                .withBlobName("testblob")
                .withContainerName("testcontainer").build();
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
        AzureTransferEndpoint endpoint = AzureTransferEndpoint.Builder.anAzureTransferEndpoint()
                .withAccount("testaccount")
                .withKey("mykey")
                .withBlobName("testblob")
                .withContainerName("testcontainer").build();
        var nifipayload = new NifiPayload(endpoint, endpoint);

        var json = typeManager.writeValueAsString(nifipayload);

        var deserialized = typeManager.readValue(json, NifiPayload.class);
        assertThat(deserialized.getSource()).hasFieldOrPropertyWithValue("key", "mykey")
                .hasFieldOrPropertyWithValue("blobName", "testblob")
                .hasFieldOrPropertyWithValue("containerName", "testcontainer")
                .hasFieldOrPropertyWithValue("account", "testaccount")
                .hasFieldOrPropertyWithValue("type", "AzureStorage")
                .hasNoNullFieldsOrProperties();

        assertThat(deserialized.getDestination()).hasFieldOrPropertyWithValue("key", "mykey")
                .hasFieldOrPropertyWithValue("blobName", "testblob")
                .hasFieldOrPropertyWithValue("containerName", "testcontainer")
                .hasFieldOrPropertyWithValue("account", "testaccount")
                .hasFieldOrPropertyWithValue("type", "AzureStorage")
                .hasNoNullFieldsOrProperties();

    }

}