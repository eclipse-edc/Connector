package org.eclipse.dataspaceconnector.iam.ion;

import org.eclipse.dataspaceconnector.iam.ion.crypto.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.ion.dto.PublicKeyDescriptor;
import org.eclipse.dataspaceconnector.iam.ion.dto.ServiceDescriptor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IonClientImplTest {

    private static final String didUrlToResolve = "did:ion:test:EiClWZ1MnE8PHjH6y4e4nCKgtKnI1DK1foZiP61I86b6pw";
    private static final String ionApiUrl = "http://23.97.144.59:3000";

    private IonClientImpl client;

    @BeforeEach
    void setup() {
        client = new IonClientImpl(ionApiUrl, new TypeManager());
    }

    @Test
    void resolve() {
        var result = client.resolve(didUrlToResolve);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(didUrlToResolve);
    }

    @Test
    void resolve_notFound() {
        assertThatThrownBy(() -> client.resolve("did:ion:test:notexist")).isInstanceOf(IonRequestException.class)
                .hasMessageContaining("404");
    }

    @Test
    void resolve_wrongPrefix() {
        assertThatThrownBy(() -> client.resolve("did:ion:foobar:notexist")).isInstanceOf(IonRequestException.class)
                .hasMessageContaining("400");
    }

    @Test
    void submitAnchorRequest() {
        var pair = KeyPairFactory.generateKeyPair();
        var pkd = PublicKeyDescriptor.Builder.create()
                .id("key-1")
                .type("EcdsaSecp256k1VerificationKey2019")
                .publicKeyJwk(Map.of("publicKeyJwk", pair.getPublicKey().toJSONString())).build();

        var sd = Collections.singletonList(ServiceDescriptor.Builder.create().id("idhub-url")
                .type("IdentityHubUrl").serviceEndpoint("https://my.identity.url").build());

        var did = client.createDid(pkd, sd);

        var didDoc = client.submit(did.create(null));
        assertThat(didDoc).isNotNull();
        assertThat(didDoc.getId()).isNotNull();
    }
}
