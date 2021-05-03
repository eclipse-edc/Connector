package com.microsoft.dagx.transfer.nifi.azureblob;

import com.microsoft.dagx.spi.types.TypeManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.junit.jupiter.api.Assertions.*;

public class AzureTransferEndpointTest {

    private TypeManager manager;
    @BeforeEach
    void setup(){
        manager= new TypeManager();
    }
    @Test
    void serialize() throws IOException {
        AzureTransferEndpoint endpoint = AzureTransferEndpoint.Builder.anAzureTransferEndpoint()
                .withAccount("testaccount")
                .withKey("mykey")
                .withBlobName("testblob")
                .withContainerName("testcontainer").build();

        var json= manager.writeValueAsString(endpoint);

        assertThat(json).isNotEmpty();
        assertThat(json).contains("testaccount")
                .contains("mykey")
                .contains("testblob")
                .contains("testcontainer")
                .contains("AzureStorage");
    }

    @Test
    void deserialize()throws IOException{
        AzureTransferEndpoint endpoint = AzureTransferEndpoint.Builder.anAzureTransferEndpoint()
                .withAccount("testaccount")
                .withKey("mykey")
                .withBlobName("testblob")
                .withContainerName("testcontainer").build();

        var json= manager.writeValueAsString(endpoint);

        var deserialized = manager.readValue(json, AzureTransferEndpoint.class);
        assertThat(deserialized).isNotNull();
        assertThat(deserialized).hasFieldOrPropertyWithValue("key", "mykey")
                .hasFieldOrPropertyWithValue("blobName", "testblob")
                .hasFieldOrPropertyWithValue("containerName", "testcontainer")
                .hasFieldOrPropertyWithValue("account", "testaccount")
                .hasFieldOrPropertyWithValue("type", "AzureStorage")
                .hasNoNullFieldsOrProperties();
    }
}