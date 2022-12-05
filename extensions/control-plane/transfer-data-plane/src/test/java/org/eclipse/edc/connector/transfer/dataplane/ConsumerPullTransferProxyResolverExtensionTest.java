/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane;

import com.nimbusds.jose.JOSEException;
import org.eclipse.edc.connector.dataplane.client.EmbeddedDataPlaneClient;
import org.eclipse.edc.connector.dataplane.client.RemoteDataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.DataPlanePublicApiUrl;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferEmbeddedProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferRemoteProxyResolver;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ConsumerPullTransferProxyResolverExtensionTest {

    private ServiceExtensionContext context;
    private ConsumerPullTransferProxyResolverExtension extension;


    @BeforeEach
    public void setUp(ServiceExtensionContext context) throws JOSEException {
        context.registerService(DataPlaneSelectorClient.class, mock(DataPlaneSelectorClient.class));

        this.context = spy(context);
        when(this.context.getMonitor()).thenReturn(mock(Monitor.class));
    }

    @Test
    void verifyProvideRemoteProxyResolverIfRemoteDataPlaneClient(ObjectFactory factory) {
        this.context.registerService(DataPlaneClient.class, mock(RemoteDataPlaneClient.class));
        this.context.registerService(DataPlanePublicApiUrl.class, mock(DataPlanePublicApiUrl.class));
        extension = factory.constructInstance(ConsumerPullTransferProxyResolverExtension.class);

        var resolver = extension.proxyResolver(context);

        assertThat(resolver)
                .isNotNull()
                .isInstanceOf(ConsumerPullTransferRemoteProxyResolver.class);
    }

    @Test
    void verifyProviderRemoteProxyResolverIfDataPlanePublicApiUrlIsNull(ObjectFactory factory) {
        this.context.registerService(DataPlaneClient.class, mock(EmbeddedDataPlaneClient.class));
        extension = factory.constructInstance(ConsumerPullTransferProxyResolverExtension.class);

        var resolver = extension.proxyResolver(context);

        assertThat(resolver)
                .isNotNull()
                .isInstanceOf(ConsumerPullTransferRemoteProxyResolver.class);
    }

    @Test
    void verifyProvideEmbeddedProxyResolver(ObjectFactory factory) {
        this.context.registerService(DataPlaneClient.class, mock(EmbeddedDataPlaneClient.class));
        this.context.registerService(DataPlanePublicApiUrl.class, mock(DataPlanePublicApiUrl.class));
        extension = factory.constructInstance(ConsumerPullTransferProxyResolverExtension.class);

        var resolver = extension.proxyResolver(context);

        assertThat(resolver)
                .isNotNull()
                .isInstanceOf(ConsumerPullTransferEmbeddedProxyResolver.class);
    }
}