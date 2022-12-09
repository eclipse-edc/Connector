/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class Oauth2ExtensionTest {

    private ServiceExtensionContext context;
    private Oauth2Extension extension;

    private CertificateResolver certificateResolver;
    private PrivateKeyResolver privateKeyResolver;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        certificateResolver = mock(CertificateResolver.class);
        privateKeyResolver = mock(PrivateKeyResolver.class);
        context.registerService(RetryPolicy.class, mock(RetryPolicy.class));
        context.registerService(CertificateResolver.class, certificateResolver);
        context.registerService(CredentialsRequestAdditionalParametersProvider.class, mock(CredentialsRequestAdditionalParametersProvider.class));
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);
        context.registerService(OkHttpClient.class, mock(OkHttpClient.class));
        extension = factory.constructInstance(Oauth2Extension.class);
        this.context = spy(context);
    }

    @Test
    void verifyExtensionWithPublicKeyDeprecatedAlias() throws CertificateEncodingException {

        var config = spy(ConfigFactory.fromMap(Map.of(
                "edc.oauth.client.id", "id",
                "edc.oauth.token.url", "url",
                "edc.oauth.public.key.alias", "alias",
                "edc.oauth.private.key.alias", "p_alias")));

        setupMocks(config);

        extension.initialize(context);

        verify(config, times(1)).getString("edc.oauth.public.key.alias");
        verify(config, never()).getString("edc.oauth.certificate.alias");

    }

    @Test
    void verifyExtensionWithCertificateAlias() throws CertificateEncodingException {

        var config = spy(ConfigFactory.fromMap(Map.of(
                "edc.oauth.client.id", "id",
                "edc.oauth.token.url", "url",
                "edc.oauth.certificate.alias", "alias",
                "edc.oauth.private.key.alias", "p_alias")));

        setupMocks(config);

        extension.initialize(context);

        verify(config, times(1)).getString("edc.oauth.certificate.alias");
        verify(config, never()).getString("edc.oauth.public.key.alias");

    }

    private void setupMocks(Config config) throws CertificateEncodingException {
        when(context.getConfig()).thenReturn(config);

        var certificate = mock(X509Certificate.class);
        var privateKey = mock(PrivateKey.class);

        when(privateKey.getAlgorithm()).thenReturn("RSA");
        when(certificate.getEncoded()).thenReturn(new byte[]{});
        when(certificateResolver.resolveCertificate("alias")).thenReturn(certificate);
        when(privateKeyResolver.resolvePrivateKey("p_alias", PrivateKey.class)).thenReturn(privateKey);
    }
}
