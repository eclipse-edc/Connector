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
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.identitytrust.core.defaults.DefaultTrustedIssuerRegistry;
import org.eclipse.edc.iam.identitytrust.core.scope.DcpScopeExtractorRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.PrivateKey;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DcpDefaultServicesExtensionTest {

    private final PrivateKeyResolver privateKeyResolver = mock();

    private static PrivateKey privateKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate()
                .toPrivateKey();
    }

    @BeforeEach
    void setup(ServiceExtensionContext context) throws JOSEException {
        var publicKeyId = "did:web:" + UUID.randomUUID() + "#key-id";
        var privateKeyAlias = "private";
        var config = ConfigFactory.fromMap(Map.of("edc.iam.sts.publickey.id", publicKeyId, "edc.iam.sts.privatekey.alias", privateKeyAlias));
        when(context.getConfig()).thenReturn(config);
        when(privateKeyResolver.resolvePrivateKey(any())).thenReturn(Result.success(privateKey()));
        context.registerService(PrivateKeyResolver.class, privateKeyResolver);
    }

    @Test
    void verify_defaultIssuerRegistry(ServiceExtensionContext context, ObjectFactory factory) {
        Monitor mockedMonitor = mock();
        context.registerService(Monitor.class, mockedMonitor);
        var ext = factory.constructInstance(DcpDefaultServicesExtension.class);

        assertThat(ext.createInMemoryIssuerRegistry()).isInstanceOf(DefaultTrustedIssuerRegistry.class);
    }

    @Test
    void verify_defaultCredentialMapperRegistry(ServiceExtensionContext context, DcpDefaultServicesExtension ext) {
        Monitor mockedMonitor = mock();
        context.registerService(Monitor.class, mockedMonitor);
        assertThat(ext.scopeExtractorRegistry()).isInstanceOf(DcpScopeExtractorRegistry.class);
    }

    @Test
    void verify_defaultAudienceResolver(DcpDefaultServicesExtension ext) {
        var id = "counterPartyId";
        var remoteMessage = mock(RemoteMessage.class);
        when(remoteMessage.getCounterPartyId()).thenReturn(id);
        assertThat(ext.defaultAudienceResolver().resolve(remoteMessage))
                .extracting(Result::getContent)
                .isEqualTo(id);
    }
}