package org.eclipse.dataspaceconnector.iam.did.credentials;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.ClientResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.RsaPublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;


class IdentityHubCredentialsVerifierTest {
    private IdentityHubClient hubClient;
    private IdentityHubCredentialsVerifier credentialsVerifier;
    private RSAPublicKey publicKey;

    @Test
    void verifyCredentials() {
        EasyMock.expect(hubClient.queryCredentials(EasyMock.isA(ObjectQueryRequest.class), EasyMock.isA(String.class), EasyMock.isA(PublicKeyWrapper.class))).andReturn(new ClientResponse<>(Map.of("region", "EU")));
        EasyMock.replay(hubClient);

        var result = credentialsVerifier.verifyCredentials("https://foo.com", new RsaPublicKeyWrapper(publicKey));
        Assertions.assertTrue(result.success());
        Assertions.assertEquals("EU", result.getValidatedCredentials().get("region"));
        EasyMock.verify(hubClient);
    }

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        publicKey = (RSAPublicKey) kp.getPublic();
        hubClient = EasyMock.createMock(IdentityHubClient.class);
        credentialsVerifier = new IdentityHubCredentialsVerifier(hubClient, new Monitor() {
        }, "did:ion:test");

    }

}
