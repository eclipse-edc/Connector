package com.microsoft.dagx.iam.oauth2;

import com.microsoft.dagx.iam.oauth2.impl.OAuth2ServiceImpl;
import com.microsoft.dagx.iam.oauth2.spi.OAuth2Service;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.function.Function;

/**
 * Provides OAuth2 client credentials flow support.
 */
public class OAuth2Extension implements ServiceExtension {

    @DagxSetting
    private static final String AUTH_KEY_URL = "dagx.oauth.auth.key.url";

    @DagxSetting
    private static final String PRIVATE_KEY_ID = "dagx.oauth.private.kid";

    @Override
    public Set<String> provides() {
        return Set.of("oauth2");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        Function<String, RSAPublicKey> publicKeySupplier = getPublicKeySupplier();

        String privateKeyId = context.getSetting(PRIVATE_KEY_ID, "testkey");
        PrivateKeyResolver privateKeyResolver = context.getService(PrivateKeyResolver.class);

        OAuth2Service oAuth2Service = new OAuth2ServiceImpl(privateKeyId, privateKeyResolver, publicKeySupplier);

        context.registerService(OAuth2Service.class, oAuth2Service);

        context.getMonitor().info("Initialized OAuth2 extension");
    }

    private Function<String, RSAPublicKey> getPublicKeySupplier() {
        return id -> null; // TODO implement using AUTH_KEY_URL
    }
}
