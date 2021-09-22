package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RSAPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RSAPublicKeyWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.dataspaceconnector.iam.did.testfixtures.TemporaryKeyLoader.loadKeys;


class GenericJweWriterReaderTest {

    @Test
    void verifyWriteRead() throws Exception {
        var key = loadKeys();
        var privateKey = key.toRSAPrivateKey();
        var publicKey = key.toRSAPublicKey();

        var objectMapper = new ObjectMapper();

        var jwe = new GenericJweWriter()
                .privateKey(new RSAPrivateKeyWrapper(privateKey))
                .publicKey(new RSAPublicKeyWrapper(publicKey))
                .objectMapper(objectMapper)
                .payload(Map.of("foo", "bar"))
                .buildJwe();

        var deserialized = new GenericJweReader().privateKey(new RSAPrivateKeyWrapper(privateKey)).mapper(objectMapper).jwe(jwe).readType(Map.class);
        Assertions.assertEquals("bar", deserialized.get("foo"));
    }
}
