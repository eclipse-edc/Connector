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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
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
    void verifyExtensionWithCertificateAlias(Oauth2ServiceExtension extension, ServiceExtensionContext context) throws CertificateEncodingException {
        var config = spy(ConfigFactory.fromMap(Map.of(
                "edc.oauth.client.id", "id",
                "edc.oauth.token.url", "url",
                "edc.oauth.certificate.alias", "alias",
                "edc.oauth.private.key.alias", "p_alias")));
        when(context.getConfig(any())).thenReturn(config);
        var certificate = mock(X509Certificate.class);
        var privateKey = mock(PrivateKey.class);
        when(privateKey.getAlgorithm()).thenReturn("RSA");
        when(certificate.getEncoded()).thenReturn(new byte[] {});
        when(certificateResolver.resolveCertificate("alias")).thenReturn(certificate);
        when(privateKeyResolver.resolvePrivateKey("p_alias", PrivateKey.class)).thenReturn(privateKey);

        extension.initialize(context);

        verify(config, times(1)).getString("edc.oauth.certificate.alias");
        verify(config, never()).getString("edc.oauth.public.key.alias");
    }

}
