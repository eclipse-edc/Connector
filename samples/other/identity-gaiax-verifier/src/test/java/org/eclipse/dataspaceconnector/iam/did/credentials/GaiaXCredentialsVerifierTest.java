package org.eclipse.dataspaceconnector.iam.did.credentials;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.ClientResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.testFixtures.TemporaryKeyLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;


class GaiaXCredentialsVerifierTest {
    private IdentityHubClient hubClient;
    private GaiaXCredentialsVerifier credentialsVerifier;
    private RSAPublicKey publicKey;

    @Test
    void verifyCredentials() {
        EasyMock.expect(hubClient.queryCredentials(EasyMock.isA(ObjectQueryRequest.class), EasyMock.isA(String.class), EasyMock.isA(PublicKey.class))).andReturn(new ClientResponse<>(Map.of("region", "EU")));
        EasyMock.replay(hubClient);

        var result = credentialsVerifier.verifyCredentials("https://foo.com", publicKey);
        Assertions.assertTrue(result.success());
        Assertions.assertEquals("EU", result.getValidatedCredentials().get("region"));
        EasyMock.verify(hubClient);
    }

    @BeforeEach
    void setUp() {
        publicKey = TemporaryKeyLoader.loadPublicKey();
        hubClient = EasyMock.createMock(IdentityHubClient.class);
        credentialsVerifier = new GaiaXCredentialsVerifier(hubClient);

    }

}
