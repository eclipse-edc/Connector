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

package org.eclipse.dataspaceconnector.catalog.cache;

import org.awaitility.Awaitility;
import org.eclipse.dataspaceconnector.catalog.cache.controller.FederatedCatalogApiController;
import org.eclipse.dataspaceconnector.catalog.cache.query.IdsMultipartNodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.directory.InMemoryNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.catalog.store.InMemoryFederatedCacheStore;
import org.eclipse.dataspaceconnector.junit.extensions.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.TEST_PROTOCOL;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createCatalog;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createNode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class FederatedCatalogCacheExtensionTest {
    private final RemoteMessageDispatcherRegistry messageDispatcherMock = mock(RemoteMessageDispatcherRegistry.class);
    private final WebService webserviceMock = mock(WebService.class);
    private final FederatedCacheStore storeMock = mock(FederatedCacheStore.class);
    private final FederatedCacheNodeDirectory nodeDirectoryMock = mock(FederatedCacheNodeDirectory.class);
    private FederatedCatalogCacheExtension extension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = spy(context);
        this.context.registerService(WebService.class, webserviceMock);
        this.context.registerService(RemoteMessageDispatcherRegistry.class, messageDispatcherMock);
        this.context.registerService(FederatedCacheNodeDirectory.class, nodeDirectoryMock);
        this.context.registerService(FederatedCacheStore.class, storeMock);
        extension = factory.constructInstance(FederatedCatalogCacheExtension.class);
    }

    @Test
    void name() {
        assertThat(extension.name()).isEqualTo("Federated Catalog Cache");
    }

    @Test
    void initialize() {
        extension.initialize(context);

        verify(webserviceMock).registerResource(any(FederatedCatalogApiController.class));
        verify(context, atLeastOnce()).getMonitor();
        verify(context).getSetting("edc.catalog.cache.partition.num.crawlers", 2);
        verify(context, times(2)).getConnectorId();
    }

    @Test
    void initialize_withHealthCheck(ObjectFactory factory) {
        var healthCheckServiceMock = mock(HealthCheckService.class);
        context.registerService(HealthCheckService.class, healthCheckServiceMock);
        extension = factory.constructInstance(FederatedCatalogCacheExtension.class); //reconstruct to honor health service
        extension.initialize(context);
        verify(healthCheckServiceMock).addReadinessProvider(any());

    }

    @Test
    void verify_successHandler_persistIsCalled() {
        when(context.getSetting(eq("edc.catalog.cache.partition.num.crawlers"), anyString())).thenReturn("1");
        when(context.getSetting(eq("edc.catalog.cache.execution.delay.seconds"), any())).thenReturn("0");
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        extension.initialize(context);
        var queryAdapter = mock(NodeQueryAdapter.class);
        when(queryAdapter.sendRequest(any())).thenReturn(CompletableFuture.completedFuture(new UpdateResponse("test-url", createCatalog())));
        extension.createNodeQueryAdapterRegistry(context).register(TEST_PROTOCOL, queryAdapter);
        extension.start();


        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(storeMock, atLeastOnce()).save(any());
                });

    }

    @Test
    void start(ServiceExtensionContext context) {
        extension.initialize(context);
    }

    @Test
    void verifyProvider_queryEngine() {

        var q = extension.getQueryEngine();
        assertThat(extension.getQueryEngine()).isSameAs(q);
    }

    @Test
    void verifyProvider_cacheNodeAdapterRegistry() {
        var n = extension.createNodeQueryAdapterRegistry(context);
        assertThat(extension.createNodeQueryAdapterRegistry(context)).isSameAs(n);
        assertThat(n.findForProtocol("ids-multipart")).hasSize(1).allSatisfy(qa -> assertThat(qa).isInstanceOf(IdsMultipartNodeQueryAdapter.class));
    }

    @Test
    void verifyProvider_defaultNodeDirectory() {
        var n = extension.defaultNodeDirectory();
        assertThat(extension.defaultNodeDirectory()).isNotSameAs(n);
        assertThat(n).isInstanceOf(InMemoryNodeDirectory.class);
    }

    @Test
    void verifyProvider_defaultCacheStore() {
        var c = extension.defaultCacheStore();
        assertThat(extension.defaultCacheStore()).isNotSameAs(c);
        assertThat(c).isInstanceOf(InMemoryFederatedCacheStore.class);
    }

}