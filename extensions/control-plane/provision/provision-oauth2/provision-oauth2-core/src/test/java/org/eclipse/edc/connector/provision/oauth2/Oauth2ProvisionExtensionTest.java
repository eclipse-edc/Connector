/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.provision.oauth2;

import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2ProvisionExtensionTest {

    private ServiceExtension extension;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(ProvisionManager.class, mock(ProvisionManager.class));
        context.registerService(ResourceManifestGenerator.class, mock(ResourceManifestGenerator.class));
        extension = factory.constructInstance(Oauth2ProvisionExtension.class);
    }

    @Test
    void shouldRegisterExtensions(ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(context.getService(TypeManager.class)).satisfies(typeManager -> {
            verify(typeManager).registerTypes(Oauth2ResourceDefinition.class, Oauth2ProvisionedResource.class);
        });
        assertThat(context.getService(ResourceManifestGenerator.class)).satisfies(resourceManifestGenerator -> {
            verify(resourceManifestGenerator).registerGenerator(isA(Oauth2ProviderResourceDefinitionGenerator.class));
            verify(resourceManifestGenerator).registerGenerator(isA(Oauth2ConsumerResourceDefinitionGenerator.class));
        });
        assertThat(context.getService(ProvisionManager.class)).satisfies(provisionManager -> {
            verify(provisionManager).register(isA(Oauth2Provisioner.class));
        });
    }
}
