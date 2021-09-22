package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.testfixtures.TemporaryKeyLoader;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RSAPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RSAPublicKeyWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;


class WriteRequestWriterReaderTest {

    @Test
    void verifyWriteRead() throws Exception {
        var key = TemporaryKeyLoader.loadKeys();
        var privateKey = key.toRSAPrivateKey();
        var publicKey = key.toRSAPublicKey();

        var objectMapper = new ObjectMapper();

        var jwe = new WriteRequestWriter()
                .privateKey(new RSAPrivateKeyWrapper(privateKey))
                .publicKey(new RSAPublicKeyWrapper(publicKey))
                .objectMapper(objectMapper)
                .commitObject(Map.of("foo", "bar"))
                .kid("kid")
                .sub("sub")
                .context("Foo")
                .type("Bar").buildJwe();

        var commit = new WriteRequestReader().privateKey(new RSAPrivateKeyWrapper(privateKey)).mapper(objectMapper).verifier((d) -> true).jwe(jwe).readCommit();

        Assertions.assertEquals("bar", ((Map<String, String>) commit.getPayload()).get("foo"));
    }


}
