/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubApiController;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.iam.did.hub.IdentityHubImpl;
import org.eclipse.dataspaceconnector.iam.did.resolution.DidPublicKeyResolverImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@ExtendWith(DependencyInjectionExtension.class)
class IdentityDidCoreExtensionTest {

    private IdentityDidCoreExtension extension;
    private WebService webserviceMock;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        var hubStore = mock(IdentityHubStore.class);
        context.registerService(IdentityHubStore.class, hubStore);
        webserviceMock = mock(WebService.class);
        context.registerService(WebService.class, webserviceMock);
        extension = factory.constructInstance(IdentityDidCoreExtension.class);
    }

    @Test
    void verifyCorrectInitialization_withPkResolverPresent(ServiceExtensionContext context) {
        context.registerService(PrivateKeyResolver.class, mock(PrivateKeyResolver.class));
        context.registerService(OkHttpClient.class, mock(OkHttpClient.class));
        context.registerService(HealthCheckService.class, mock(HealthCheckService.class));

        extension.initialize(context);

        assertThat(context.getService(DidResolverRegistry.class)).isInstanceOf(DidResolverRegistry.class);
        assertThat(context.getService(DidPublicKeyResolver.class)).isInstanceOf(DidPublicKeyResolverImpl.class);
        assertThat(context.getService(IdentityHub.class)).isInstanceOf(IdentityHubImpl.class);
        assertThat(context.getService(IdentityHubClient.class)).isInstanceOf(IdentityHubClientImpl.class);
        verify(webserviceMock).registerResource(isA(IdentityHubApiController.class));
    }
}
