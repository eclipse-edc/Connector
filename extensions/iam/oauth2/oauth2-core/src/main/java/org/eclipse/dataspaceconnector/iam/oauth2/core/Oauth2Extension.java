/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.core.impl.DefaultJwtDecorator;
import org.eclipse.dataspaceconnector.iam.oauth2.core.impl.IdentityProviderKeyResolver;
import org.eclipse.dataspaceconnector.iam.oauth2.core.impl.JwtDecoratorRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.impl.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.core.impl.Oauth2ServiceImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.JwtDecorator;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides OAuth2 client credentials flow support.
 */
@Provides({ IdentityService.class, JwtDecoratorRegistry.class })
public class Oauth2Extension implements ServiceExtension {

    private static final long TOKEN_EXPIRATION = TimeUnit.MINUTES.toSeconds(5);

    @EdcSetting
    private static final String PROVIDER_JWKS_URL = "edc.oauth.provider.jwks.url";

    @EdcSetting
    private static final String PROVIDER_AUDIENCE = "edc.oauth.provider.audience";

    @EdcSetting
    private static final String PUBLIC_KEY_ALIAS = "edc.oauth.public.key.alias";

    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "edc.oauth.private.key.alias";

    @EdcSetting
    private static final String PROVIDER_JWKS_REFRESH = "edc.oauth.provider.jwks.refresh"; // in minutes

    @EdcSetting
    private static final String TOKEN_URL = "edc.oauth.token.url";

    @EdcSetting
    private static final String CLIENT_ID = "edc.oauth.client.id";

    private IdentityProviderKeyResolver providerKeyResolver;

    private long keyRefreshInterval;

    private ScheduledExecutorService executorService;

    @Inject
    private OkHttpClient okHttpClient;

    private static JWSSigner createTokenSigner(Oauth2Configuration configuration) {
        var pkId = configuration.getPrivateKeyAlias();
        var pk = configuration.getPrivateKeyResolver().resolvePrivateKey(pkId, PrivateKey.class);

        if (pk == null) {
            throw new EdcException("Failed to resolve private key, required for JWSSigner.");
        } else if ("EC".equals(pk.getAlgorithm())) {
            //supports ECDSA private key
            try {
                return new ECDSASigner((ECPrivateKey) pk);
            } catch (JOSEException e) {
                throw new EdcException("Failed to load JWSSigner for EC private key: " + e);
            }
        } else {
            //default: RSA private key
            return new RSASSASigner(pk);
        }
    }

    private static byte[] getEncodedClientCertificate(Oauth2Configuration configuration) {
        X509Certificate certificate = configuration.getCertificateResolver().resolveCertificate(configuration.getPublicCertificateAlias());
        if (certificate == null) {
            throw new EdcException("Public certificate not found: " + configuration.getPublicCertificateAlias());
        }
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new EdcException("Failed to encode certificate: " + e);
        }
    }

    @Override
    public String name() {
        return "OAuth2";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        // setup the provider key resolver, which will be scheduled for refresh at runtime start
        String jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        providerKeyResolver = new IdentityProviderKeyResolver(jwksUrl, context.getMonitor(), okHttpClient, context.getTypeManager());
        keyRefreshInterval = Integer.parseInt(context.getSetting(PROVIDER_JWKS_REFRESH, "5"));

        Oauth2Configuration configuration = createConfig(context);

        // create the decorator registry
        JwtDecoratorRegistry jwtDecoratorRegistry = new JwtDecoratorRegistryImpl();
        JwtDecorator defaultDecorator = new DefaultJwtDecorator(configuration.getProviderAudience(), configuration.getClientId(), getEncodedClientCertificate(configuration), TOKEN_EXPIRATION);
        jwtDecoratorRegistry.register(defaultDecorator);
        context.registerService(JwtDecoratorRegistry.class, jwtDecoratorRegistry);

        // supports RSA and EC private keys
        JWSSigner tokenSigner = createTokenSigner(configuration);
        IdentityService oauth2Service = new Oauth2ServiceImpl(configuration, tokenSigner, okHttpClient, jwtDecoratorRegistry, context.getTypeManager());

        context.registerService(IdentityService.class, oauth2Service);
    }

    @Override
    public void start() {
        // refresh the provider keys at start, then schedule a refresh on a periodic basis according to the configured interval
        providerKeyResolver.refreshKeys();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> providerKeyResolver.refreshKeys(), keyRefreshInterval, keyRefreshInterval, TimeUnit.MINUTES);
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private Oauth2Configuration createConfig(ServiceExtensionContext context) {
        String tokenUrl = context.getSetting(TOKEN_URL, null);
        String providerAudience = context.getSetting(PROVIDER_AUDIENCE, context.getConnectorId());
        String publicKeyAlias = context.getSetting(PUBLIC_KEY_ALIAS, null);
        String privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, null);
        String clientId = context.getSetting(CLIENT_ID, null);
        PrivateKeyResolver privateKeyResolver = context.getService(PrivateKeyResolver.class);
        CertificateResolver certificateResolver = context.getService(CertificateResolver.class);
        return Oauth2Configuration.Builder.newInstance()
                .identityProviderKeyResolver(providerKeyResolver)
                .tokenUrl(tokenUrl)
                .providerAudience(providerAudience)
                .publicCertificateAlias(publicKeyAlias)
                .privateKeyAlias(privateKeyAlias)
                .clientId(clientId)
                .privateKeyResolver(privateKeyResolver)
                .certificateResolver(certificateResolver).build();
    }
}
