package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.dataspaceconnector.iam.did.hub.test.JweTestFunctions.loadAndGetKey;

/**
 *
 */
class GenericJweWriterReaderTest {

    @Test
    void verifyWriteRead() throws Exception {
        var key = loadAndGetKey();
        var privateKey = key.toRSAPrivateKey();
        var publicKey = key.toRSAPublicKey();

        var objectMapper = new ObjectMapper();

        var jwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .payload(Map.of("foo", "bar"))
                .buildJwe();

        var deserialized = new GenericJweReader().privateKey(privateKey).mapper(objectMapper).jwe(jwe).readType(Map.class);
        Assertions.assertEquals("bar", deserialized.get("foo"));
    }
}
