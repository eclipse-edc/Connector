package org.eclipse.dataspaceconnector.iam.did.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.testFixtures.TemporaryKeyLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 *
 */
class IdentityHubClientImplTest {
    private IdentityHubClientImpl hubClient;

    @Test
    @Disabled
    void verifyClientCredentials() {
        var query = ObjectQuery.Builder.newInstance().context("GAIA-X").type("RegistrationCredentials").build();
        var queryRequest = ObjectQueryRequest.Builder.newInstance().query(query).iss("123").aud("aud").sub("sub").build();

        // TODO pull the hub url from the DID document
        var hubBaseUrl = "http://localhost:8181/api/identity-hub/";
        var publicKey = TemporaryKeyLoader.loadPublicKey();
        var credentials = hubClient.queryCredentials(queryRequest, hubBaseUrl, publicKey);
        Assertions.assertNotNull(credentials);
    }

    @BeforeEach
    void setUp() {
        hubClient = new IdentityHubClientImpl(TemporaryKeyLoader::loadPrivateKey, new OkHttpClient(), new ObjectMapper());
    }
}
