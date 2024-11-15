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
 *       sovity GmbH - added issuedAt leeway
 *
 */

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.keys.spi.CertificateResolver;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2ServiceExtensionTest {

    private final CertificateResolver certificateResolver = mock();
    private final PrivateKeyResolver privateKeyResolver = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(CertificateResolver.class, certificateResolver);
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);
    }

    @Test
    void verifyExtensionWithCertificateAlias(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var config = spy(ConfigFactory.fromMap(Map.of(
                "edc.oauth.client.id", "id",
                "edc.oauth.token.url", "url",
                "edc.oauth.certificate.alias", "alias",
                "edc.oauth.private.key.alias", "p_alias")));
        mockCertificate("alias");
        mockRsaPrivateKey("p_alias");

        when(context.getConfig(any())).thenReturn(config);
        objectFactory.constructInstance(Oauth2ServiceExtension.class).initialize(context);

        verify(config, times(1)).getString("edc.oauth.certificate.alias");
        verify(config, never()).getString("edc.oauth.public.key.alias");
    }

    @Test
    void leewayWarningLoggedWhenLeewayUnconfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var config = spy(ConfigFactory.fromMap(Map.of(
                "edc.oauth.client.id", "id",
                "edc.oauth.token.url", "url",
                "edc.oauth.certificate.alias", "alias",
                "edc.oauth.private.key.alias", "p_alias")));
        mockCertificate("alias");
        mockRsaPrivateKey("p_alias");

        var monitor = mock(Monitor.class);
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getConfig()).thenReturn(config);
        objectFactory.constructInstance(Oauth2ServiceExtension.class).initialize(context);

        var message = "No value was configured for 'edc.oauth.validation.issued.at.leeway'.";
        verify(monitor, times(1)).info(contains(message));
    }

    @Test
    void leewayNoWarningWhenLeewayConfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var config = spy(ConfigFactory.fromMap(Map.of(
                "edc.oauth.client.id", "id",
                "edc.oauth.token.url", "url",
                "edc.oauth.certificate.alias", "alias",
                "edc.oauth.private.key.alias", "p_alias",
                "edc.oauth.validation.issued.at.leeway", "5")));
        mockCertificate("alias");
        mockRsaPrivateKey("p_alias");

        var monitor = mock(Monitor.class);
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getConfig(any())).thenReturn(config);
        objectFactory.constructInstance(Oauth2ServiceExtension.class).initialize(context);

        var message = "No value was configured for 'edc.oauth.validation.issued.at.leeway'.";
        verify(monitor, never()).info(contains(message));
    }

    private void mockRsaPrivateKey(String alias) {
        var privateKey = mock(PrivateKey.class);
        when(privateKey.getAlgorithm()).thenReturn("RSA");
        when(privateKeyResolver.resolvePrivateKey(alias)).thenReturn(Result.success(privateKey));
    }

    private void mockCertificate(String alias) {
        try {
            var certificate = mock(X509Certificate.class);
            when(certificate.getEncoded()).thenReturn(new byte[]{});
            when(certificateResolver.resolveCertificate(alias)).thenReturn(certificate);
        } catch (CertificateEncodingException e) {
            // Should never happen, it's a checked exception in the way of mocking
            throw new RuntimeException(e);
        }
    }
}
