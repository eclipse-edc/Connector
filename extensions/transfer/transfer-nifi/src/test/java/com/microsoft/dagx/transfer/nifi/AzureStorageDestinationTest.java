package com.microsoft.dagx.transfer.nifi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class AzureStorageDestinationTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        DestinationSecretToken token = new DestinationSecretToken("token", 1L);
        AzureStorageDestination process = AzureStorageDestination.Builder.newInstance().secretToken(token).account("account").blobname("blob").container("container").key("key").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        AzureStorageDestination deserialized = mapper.readValue(writer.toString(), AzureStorageDestination.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.getSecretToken());
        assertNotNull(deserialized.getType());
        assertEquals("account", deserialized.getAccount());
        assertEquals("blob", deserialized.getBlobname());
        assertEquals("container", deserialized.getContainer());
        assertEquals("key", deserialized.getKey());

    }
}
