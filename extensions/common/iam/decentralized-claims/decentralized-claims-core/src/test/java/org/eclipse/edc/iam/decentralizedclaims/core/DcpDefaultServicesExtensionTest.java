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

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.DefaultTrustedIssuerRegistry;
import org.eclipse.edc.iam.decentralizedclaims.core.scope.DcpScopeExtractorRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DcpDefaultServicesExtensionTest {

    private final PrivateKeyResolver privateKeyResolver = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        var publicKeyId = "did:web:" + UUID.randomUUID() + "#key-id";
        var privateKeyAlias = "private";
        var config = ConfigFactory.fromMap(Map.of("edc.iam.sts.publickey.id", publicKeyId, "edc.iam.sts.privatekey.alias", privateKeyAlias));
        when(context.getConfig()).thenReturn(config);
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
        var message = new TestMessage(id);

        var result = ext.defaultAudienceResolver().resolve(message);

        assertThat(result).isSucceeded().isEqualTo(id);
    }

    private static class TestMessage extends ProtocolRemoteMessage {
        private final String counterPartyId;

        TestMessage(String counterPartyId) {
            this.counterPartyId = counterPartyId;
        }

        @Override
        public String getCounterPartyId() {
            return counterPartyId;
        }
    }
}
