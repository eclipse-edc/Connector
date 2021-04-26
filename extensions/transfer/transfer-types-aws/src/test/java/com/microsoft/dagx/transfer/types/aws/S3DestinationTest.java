package com.microsoft.dagx.transfer.types.aws;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
class S3DestinationTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // this is actually necessary, because otherwise the serialize-only "type" field would
        // raise an UnrecognizedPropertyException
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        DestinationSecretToken token = new DestinationSecretToken("token", 1L);
        S3Destination process = S3Destination.Builder.newInstance().secretToken(token).region("region").bucketName("bucket").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        S3Destination deserialized = mapper.readValue(writer.toString(), S3Destination.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.getSecretToken());
        assertNotNull(deserialized.getType());
        assertEquals("region", deserialized.getRegion());
        assertEquals("bucket", deserialized.getBucketName());
    }
}
