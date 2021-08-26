package org.eclipse.dataspaceconnector.samples.identity.did;

import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.ion.crypto.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.PublicKeyJwk;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.Service;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.ServiceEndpoint;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.VerificationMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDidDocumentStoreTest {

    private InMemoryDidDocumentStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryDidDocumentStore();
    }

    @Test
    void getAll() {
        DidDocument doc = createDidDocument();
        store.save(doc);
        assertThat(store.getAll(100)).hasSize(1).containsOnly(doc);
    }

    @Test
    void getAll_whenNoneExist() {
        assertThat(store.getAll(100)).isNotNull().isEmpty();
    }

    @Test
    void getAll_exceedsLimit() {
        for (int i = 0; i < 10; i++) {
            store.save(createDidDocument());
        }
        assertThat(store.getAll(5)).hasSize(5);
    }

    @Test
    void getAfter() {
        var d1 = createDidDocument();
        var d2 = createDidDocument();
        var d3 = createDidDocument();
        store.save(d1);
        store.save(d2);
        store.save(d3);

        assertThat(store.getAfter(d2.getId())).hasSize(2).containsOnly(d2, d3);
    }

    @Test
    void getAfter_whenEmpty() {
        assertThat(store.getAfter("nonexist")).isNotNull().isEmpty();
    }

    @Test
    void getAfter_whenNotExist() {
        store.save(createDidDocument());
        store.save(createDidDocument());
        store.save(createDidDocument());

        assertThat(store.getAfter("notexist")).isNotNull().isEmpty();
    }

    @Test
    void save() {
        var did = createDidDocument();
        store.save(did);

        assertThat(store.getAll(100)).hasSize(1).containsOnly(did);
    }

    @Test
    void save_alreadyExists() {
        var did = createDidDocument();
        assertThat(store.save(did)).isTrue();
        assertThat(store.save(did)).isFalse();

        assertThat(store.getAll(100)).hasSize(1).containsOnly(did);
    }

    @Test
    void getLatest() throws InterruptedException {
        var did1 = createDidDocument();
        var did2 = createDidDocument();
        var did3 = createDidDocument();
        var did4 = createDidDocument();

        store.save(did1);
        Thread.sleep(10);
        store.save(did2);
        Thread.sleep(10);
        store.save(did3);
        Thread.sleep(10);
        store.save(did4);

        assertThat(store.getLatest()).isEqualTo(did4);
    }

    @Test
    void getLatest_whenNoneExist() {

        assertThat(store.getLatest()).isNull();
    }

    private DidDocument createDidDocument() {
        Random random = new SecureRandom();
        byte[] r = new byte[32]; //Means 2048 bit
        random.nextBytes(r);
        String s = Base64.getEncoder().encodeToString(r);
        var serviceEndpoint = new ServiceEndpoint("someschema","SomeEndpoint", List.of("https://test.service.com"));
        var service = new Service("#domain-1", "LinkedDomains", serviceEndpoint);
        return DidDocument.Builder.newInstance()
                .id("did:ion:" + s)
                .authentication(Collections.singletonList("#key-1"))
                .services(Collections.singletonList(service))
                .verificationMethod(Collections.singletonList(createVerificationMethod()))
                .build();
    }

    private VerificationMethod createVerificationMethod() {
        var publicKey = (ECKey) KeyPairFactory.generateKeyPair().getPublicKey();
        return VerificationMethod.Builder.create()
                .controller("")
                .id("#key-1")
                .type("EcdsaSecp256k1VerificationKey2019")
                .publicKeyJwk(new PublicKeyJwk(publicKey.getCurve().getName(), publicKey.getKeyType().getValue(), publicKey.getX().toString(), publicKey.getY().toString()))
                .build();
    }
}
