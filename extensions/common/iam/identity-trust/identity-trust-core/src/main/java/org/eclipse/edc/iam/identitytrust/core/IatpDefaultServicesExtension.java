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

import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.KeyPairFactory;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.security.KeyPair;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Extension("Identity And Trust Extension to register default services")
public class IatpDefaultServicesExtension implements ServiceExtension {

    @Setting(value = "Alias of private key used for signing tokens, retrieved from private key resolver", defaultValue = "A random EC private key")
    public static final String STS_PRIVATE_KEY_ALIAS = "edc.iam.sts.privatekey.alias";
    @Setting(value = "Alias of public key used for verifying the tokens, retrieved from the vault", defaultValue = "A random EC public key")
    public static final String STS_PUBLIC_KEY_ALIAS = "edc.iam.sts.publickey.alias";
    // not a setting, it's defined in Oauth2ServiceExtension
    private static final String OAUTH_TOKENURL_PROPERTY = "edc.oauth.token.url";
    @Setting(value = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + IatpDefaultServicesExtension.DEFAULT_STS_TOKEN_EXPIRATION_MIN)
    private static final String STS_TOKEN_EXPIRATION = "edc.iam.sts.token.expiration"; // in minutes

    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;

    @Inject
    private KeyPairFactory keyPairFactory;

    @Inject
    private Clock clock;

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

    @Provider(isDefault = true)
    public TrustedIssuerRegistry createInMemoryIssuerRegistry() {
        return new DefaultTrustedIssuerRegistry();
    }
}
