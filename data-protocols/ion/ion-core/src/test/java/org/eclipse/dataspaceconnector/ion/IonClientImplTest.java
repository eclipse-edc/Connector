package org.eclipse.dataspaceconnector.ion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.ion.model.IonRequestFactory;
import org.eclipse.dataspaceconnector.ion.model.PublicKeyDescriptor;
import org.eclipse.dataspaceconnector.ion.model.ServiceDescriptor;
import org.eclipse.dataspaceconnector.ion.util.KeyPairFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class IonClientImplTest {

    private static final String didUrlToResolve = "did:ion:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA";


    private DefaultIonClient client;

    @BeforeEach
    void setup() {
        client = new DefaultIonClient(new ObjectMapper());
    }

    @Test
    void resolve() {
        var result = client.resolve(didUrlToResolve);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(didUrlToResolve);
    }


    @Test
    void resolve_notFound() {
        assertThatThrownBy(() -> client.resolve("did:ion:notexist")).isInstanceOf(IonRequestException.class)
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

        var content = new HashMap<String, Object>();
        content.put("publicKeys", Collections.singletonList(pkd));
        content.put("services", sd);
        var createDidRequest = IonRequestFactory.createCreateRequest(KeyPairFactory.generateKeyPair().getPublicKey(), KeyPairFactory.generateKeyPair().getPublicKey(), content);
        assertThat(createDidRequest.getDidUri()).isNotNull();
        assertThat(createDidRequest.getSuffix()).isNotNull();
        assertThat(createDidRequest.getDidUri()).endsWith(createDidRequest.getSuffix());

        //TODO: commented out because anchoring does not yet work due to rate limiting

//        var didDoc = client.submit(createDidRequest);
//        assertThat(didDoc).isNotNull();
//        assertThat(didDoc.getId()).isNotNull();
    }
}
