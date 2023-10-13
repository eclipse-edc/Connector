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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyPairFactory;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.core.IatpDefaultServicesExtension.STS_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.iam.identitytrust.core.IatpDefaultServicesExtension.STS_PUBLIC_KEY_ALIAS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class IatpDefaultServicesExtensionTest {

    private final KeyPairFactory keyPairFactory = mock();
    private final KeyPair keypair = mock();

    private static PrivateKey privateKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate()
                .toPrivateKey();
    }

    @BeforeEach
    void setup(ServiceExtensionContext context) throws JOSEException {
        context.registerService(KeyPairFactory.class, keyPairFactory);
        when(keypair.getPrivate()).thenReturn(privateKey());
    }

    @Test
    void verify_defaultService(ServiceExtensionContext context, IatpDefaultServicesExtension ext) {
        var publicAlias = "public";
        var privateAlias = "private";
        Monitor mockedMonitor = mock();
        context.registerService(Monitor.class, mockedMonitor);
        when(context.getSetting(STS_PUBLIC_KEY_ALIAS, null)).thenReturn(publicAlias);
        when(context.getSetting(STS_PRIVATE_KEY_ALIAS, null)).thenReturn(privateAlias);
        when(keyPairFactory.fromConfig(publicAlias, privateAlias)).thenReturn(Result.success(keypair));
        var sts = ext.createDefaultTokenService(context);

        assertThat(sts).isInstanceOf(EmbeddedSecureTokenService.class);
        verify(mockedMonitor).info(anyString());

        verify(keyPairFactory, never()).defaultKeyPair();
    }

    @Test
    void verify_defaultServiceWithWarning(ServiceExtensionContext context, IatpDefaultServicesExtension ext) {
        Monitor mockedMonitor = mock();
        context.registerService(Monitor.class, mockedMonitor);
        when(context.getSetting(eq("edc.oauth.token.url"), any())).thenReturn("https://some.url");
        when(keyPairFactory.defaultKeyPair()).thenReturn(keypair);

        ext.createDefaultTokenService(context);

        verify(mockedMonitor).info(anyString());
        verify(mockedMonitor).warning(anyString());
        verify(keyPairFactory, times(1)).defaultKeyPair();
    }
}