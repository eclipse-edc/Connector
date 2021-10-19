package org.eclipse.dataspaceconnector.ion.model.did.resolution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidResolveResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class DidDocumentSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void deserialization() throws JsonProcessingException {
        var json = readFile("diddocument.json");

        var didResp = objectMapper.readValue(json, DidResolveResponse.class);

        assertThat(didResp).isNotNull();
        assertThat(didResp.getDidDocument()).isNotNull();
        assertThat(didResp.getDidDocumentMetadata()).isNotNull();

        assertThat(didResp.getDidDocument().getService()).hasSize(1);
        assertThat(didResp.getDidDocument().getVerificationMethod()).hasSize(1);
        assertThat(didResp.getDidDocument().getVerificationMethod().get(0))
                .hasFieldOrPropertyWithValue("id", "#my-key-1")
                .hasFieldOrPropertyWithValue("controller", "")
                .hasFieldOrPropertyWithValue("type", "EcdsaSecp256k1VerificationKey2019");
    }

    private String readFile(String resourceName) {
        var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";

    }
}
