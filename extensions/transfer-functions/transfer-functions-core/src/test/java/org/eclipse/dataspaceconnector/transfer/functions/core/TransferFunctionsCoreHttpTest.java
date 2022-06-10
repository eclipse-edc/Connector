/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.functions.core;

import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.retry.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.functions.spi.flow.http.TransferFunctionInterceptorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.functions.core.TransferFunctionsCoreServiceExtension.ENABLED_PROTOCOLS_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the HTTP flow controller works.
 */
@ExtendWith(EdcExtension.class)
public class TransferFunctionsCoreHttpTest {

    @Test
    void verifyHttpFlowControllerInvoked(TransferProcessManager processManager, TransferFunctionInterceptorRegistry registry) throws InterruptedException {

        var latch = new CountDownLatch(1);

        registry.registerHttpInterceptor(chain -> {
            latch.countDown();
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1).code(200)
                    .body(ResponseBody.create("", MediaType.get("application/json"))).message("")
                    .build();
        });

        var asset = Asset.Builder.newInstance().id("test123").build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("ids")
                .destinationType("foo")
                .assetId(asset.getId())
                .managedResources(false)
                .dataDestination(DataAddress.Builder.newInstance().type("test-protocol1").build())
                .connectorId("test").build();

        processManager.initiateProviderRequest(dataRequest);

        assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue();
    }

    @BeforeEach
    protected void before(EdcExtension extension) {
        extension.setConfiguration(Map.of(ENABLED_PROTOCOLS_KEY, "test-protocol1"));

        // register a wait strategy of 1ms to speed up the interval between transfer manager iterations
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);

        extension.registerSystemExtension(ServiceExtension.class, new MockServiceExtension());
    }

    @Provides({ DataAddressResolver.class })
    private static class MockServiceExtension implements ServiceExtension {

        // TODO remove this when https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/569 is resolved
        @Override
        public void initialize(ServiceExtensionContext context) {
            var addressResolver = mock(DataAddressResolver.class);
            when(addressResolver.resolveForAsset(any())).thenReturn(DataAddress.Builder.newInstance().type("test").build());
            context.registerService(DataAddressResolver.class, addressResolver);
        }
    }
}
