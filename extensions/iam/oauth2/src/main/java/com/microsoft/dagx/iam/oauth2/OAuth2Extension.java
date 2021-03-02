package com.microsoft.dagx.iam.oauth2;

import com.microsoft.dagx.iam.oauth2.impl.OAuth2Configuration;
import com.microsoft.dagx.iam.oauth2.impl.OAuth2ServiceImpl;
import com.microsoft.dagx.iam.oauth2.spi.OAuth2Service;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 * Provides OAuth2 client credentials flow support.
 */
public class OAuth2Extension implements ServiceExtension {

    @DagxSetting
    private static final String PUBLIC_KEY_ALIAS = "dagx.oauth.public.key.alias";

    @DagxSetting
    private static final String PRIVATE_KEY_ALIAS = "dagx.oauth.private.key.alias";

    @DagxSetting
    private static final String AUTH_PUBLIC_KEY_URL = "dagx.oauth.public.key.url";

    @DagxSetting
    private static final String TOKEN_URL = "dagx.oauth.token.url";

    @DagxSetting
    private static final String CLIENT_ID = "dagx.oauth.client.id";

    @DagxSetting
    private static final String SCOPE = "dagx.oauth.scope";

    @Override
    public Set<String> provides() {
        return Set.of("oauth2");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        OAuth2Configuration.Builder configBuilder =  OAuth2Configuration.Builder.newInstance();

        CertificateResolver providerResolver = getProviderCertificateResolver();
        configBuilder.identityProviderCertificateResolver(providerResolver);

        String publicKeyAlias = context.getSetting(PUBLIC_KEY_ALIAS, "publickeyalias");
        configBuilder.publicCertificateAlias(publicKeyAlias);

        String privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privatekeyalias");
        configBuilder.publicCertificateAlias(privateKeyAlias);

        String tokenUrl = context.getSetting(TOKEN_URL, "tokenurl");
        configBuilder.tokenUrl(tokenUrl);

        String clientId = context.getSetting(CLIENT_ID, "clientid");
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

    private CertificateResolver getProviderCertificateResolver() {
        return id -> null; // TODO implement using AUTH_PUBLIC_KEY_URL
    }
}
