package com.microsoft.dagx.iam.oauth2;

import com.microsoft.dagx.iam.oauth2.impl.IdentityProviderKeyResolver;
import com.microsoft.dagx.iam.oauth2.impl.OAuth2Configuration;
import com.microsoft.dagx.iam.oauth2.impl.OAuth2ServiceImpl;
import com.microsoft.dagx.iam.oauth2.spi.OAuth2Service;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Provides OAuth2 client credentials flow support.
 */
public class OAuth2Extension implements ServiceExtension {

    @DagxSetting
    private static final String PUBLIC_KEY_ALIAS = "dagx.oauth.public.key.alias";

    @DagxSetting
    private static final String PRIVATE_KEY_ALIAS = "dagx.oauth.private.key.alias";

    @DagxSetting
    private static final String PROVIDER_JWKS_URL = "dagx.oauth.provider.jwks.url";

    @DagxSetting
    private static final String PROVIDER_JWKS_REFRESH = "dagx.oauth.provider.jwks.refresh"; // in minutes

    @DagxSetting
    private static final String TOKEN_URL = "dagx.oauth.token.url";

    @DagxSetting
    private static final String CLIENT_ID = "dagx.oauth.client.id";

    @DagxSetting
    private static final String SCOPE = "dagx.oauth.scope";

    private IdentityProviderKeyResolver providerKeyResolver;
    private long keyRefreshInterval;

    @Override
    public Set<String> provides() {
        return Set.of("oauth2");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // setup the provider key resolver, which will be scheduled for refresh at runtime start
        String jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        providerKeyResolver = new IdentityProviderKeyResolver(jwksUrl, context.getMonitor());
        keyRefreshInterval = Integer.parseInt(context.getSetting(PROVIDER_JWKS_REFRESH, "5"));

        // setup the OAuth2 service
        OAuth2Configuration.Builder configBuilder = OAuth2Configuration.Builder.newInstance();

        configBuilder.identityProviderKeyResolver(providerKeyResolver);

        String publicKeyAlias = context.getSetting(PUBLIC_KEY_ALIAS, "publicKeyAlias");
        configBuilder.publicCertificateAlias(publicKeyAlias);

        String privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privateKeyAlias");
        configBuilder.publicCertificateAlias(privateKeyAlias);

        String tokenUrl = context.getSetting(TOKEN_URL, "tokenUrl");
        configBuilder.tokenUrl(tokenUrl);

        String clientId = context.getSetting(CLIENT_ID, "clientId");
        configBuilder.tokenUrl(clientId);

        String scope = context.getSetting(SCOPE, "scope");
        configBuilder.tokenUrl(scope);

        PrivateKeyResolver privateKeyResolver = context.getService(PrivateKeyResolver.class);
        configBuilder.privateKeyResolver(privateKeyResolver);

        CertificateResolver certificateResolver = context.getService(CertificateResolver.class);
        configBuilder.certificateResolver(certificateResolver);

        configBuilder.objectMapper(context.getTypeManager().getMapper());

        OAuth2Configuration configuration = configBuilder.build();

        OAuth2Service oAuth2Service = new OAuth2ServiceImpl(configuration);

        context.registerService(OAuth2Service.class, oAuth2Service);

        context.getMonitor().info("Initialized OAuth2 extension");
    }

    @Override
    public void start() {
        // refresh the provider keys at start, then schedule a refresh on a periodic basis according to the configured interval
        providerKeyResolver.refreshKeys();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> providerKeyResolver.refreshKeys(), keyRefreshInterval, keyRefreshInterval, TimeUnit.MINUTES);
    }
}
