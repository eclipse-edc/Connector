/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.core;

import jakarta.json.Json;
import org.eclipse.edc.iam.identitytrust.core.defaults.DefaultCredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.core.defaults.DefaultTrustedIssuerRegistry;
import org.eclipse.edc.iam.identitytrust.core.defaults.InMemorySignatureSuiteRegistry;
import org.eclipse.edc.iam.identitytrust.core.scope.IatpScopeExtractorRegistry;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.scope.ScopeExtractorRegistry;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.security.KeyPairFactory;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.security.KeyPair;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension("Identity And Trust Extension to register default services")
public class IatpDefaultServicesExtension implements ServiceExtension {

    @Setting(value = "Alias of private key used for signing tokens, retrieved from private key resolver", defaultValue = "A random EC private key")
    public static final String STS_PRIVATE_KEY_ALIAS = "edc.iam.sts.privatekey.alias";
    @Setting(value = "Alias of public key used for verifying the tokens, retrieved from the vault", defaultValue = "A random EC public key")
    public static final String STS_PUBLIC_KEY_ALIAS = "edc.iam.sts.publickey.alias";
    @Setting(value = "URL of the CredentialService used to present credentials", required = true)
    public static final String CREDENTIALSERVICE_URL_PROPERTY = "edc.iam.credentialservice.url";
    // not a setting, it's defined in Oauth2ServiceExtension
    private static final String OAUTH_TOKENURL_PROPERTY = "edc.oauth.token.url";
    @Setting(value = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + IatpDefaultServicesExtension.DEFAULT_STS_TOKEN_EXPIRATION_MIN)
    private static final String STS_TOKEN_EXPIRATION = "edc.iam.sts.token.expiration"; // in minutes
    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;

    @Inject
    private KeyPairFactory keyPairFactory;

    @Inject
    private Clock clock;

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private JsonLd jsonLd;
    
    @Provider(isDefault = true)
    public SecureTokenService createDefaultTokenService(ServiceExtensionContext context) {
        context.getMonitor().info("Using the Embedded STS client, as no other implementation was provided.");
        var keyPair = keyPairFromConfig(context);
        var tokenExpiration = context.getSetting(STS_TOKEN_EXPIRATION, DEFAULT_STS_TOKEN_EXPIRATION_MIN);


        if (context.getSetting(OAUTH_TOKENURL_PROPERTY, null) != null) {
            context.getMonitor().warning("The property '%s' was configured, but no remote SecureTokenService was found on the classpath. ".formatted(OAUTH_TOKENURL_PROPERTY) +
                    "This could be an indicator of a configuration problem.");
        }

        return new EmbeddedSecureTokenService(new TokenGenerationServiceImpl(keyPair.getPrivate()), clock, TimeUnit.MINUTES.toSeconds(tokenExpiration));
    }

    @Provider(isDefault = true)
    public TrustedIssuerRegistry createInMemoryIssuerRegistry() {
        return new DefaultTrustedIssuerRegistry();
    }

    @Provider(isDefault = true)
    public SignatureSuiteRegistry createSignatureSuiteRegistry() {
        return new InMemorySignatureSuiteRegistry();
    }

    @Provider(isDefault = true)
    public ScopeExtractorRegistry scopeExtractorRegistry() {
        return new IatpScopeExtractorRegistry();
    }

    @Provider(isDefault = true)
    public CredentialServiceClient createClient(ServiceExtensionContext context) {
        return new DefaultCredentialServiceClient(httpClient, Json.createBuilderFactory(Map.of()),
                typeManager.getMapper(JSON_LD), typeTransformerRegistry, jsonLd, context.getMonitor(),
                context.getConfig().getString(CREDENTIALSERVICE_URL_PROPERTY));
    }

    private KeyPair keyPairFromConfig(ServiceExtensionContext context) {
        var pubKeyAlias = context.getSetting(STS_PUBLIC_KEY_ALIAS, null);
        var privKeyAlias = context.getSetting(STS_PRIVATE_KEY_ALIAS, null);
        if (pubKeyAlias == null && privKeyAlias == null) {
            context.getMonitor().info(() -> "No public or private key provided for 'STS.' A key pair will be generated (DO NOT USE IN PRODUCTION)");
            return keyPairFactory.defaultKeyPair();
        }
        Objects.requireNonNull(pubKeyAlias, "public key alias");
        Objects.requireNonNull(privKeyAlias, "private key alias");
        return keyPairFactory.fromConfig(pubKeyAlias, privKeyAlias)
                .orElseThrow(failure -> new EdcException(failure.getFailureDetail()));
    }
}
