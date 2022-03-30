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
package org.eclipse.dataspaceconnector.transfer.provision.http;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.retry.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.PROVISIONER_CONFIG;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.TEST_DATA_TYPE;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.createResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class HttpProvisionerExtensionEndToEndTest {

    private Interceptor delegate;

    /**
     * Tests the case where an initial request returns a retryable failure and the second request completes.
     */
    @Test
    void processProviderRequestRetry(TransferProcessManager processManager, AssetLoader loader, TransferProcessStore store) throws Exception {
        var latch = new CountDownLatch(1);

        when(delegate.intercept(any()))
                .thenAnswer(invocation -> createResponse(503, invocation))
                .thenAnswer(invocation -> {
                    latch.countDown();
                    return createResponse(200, invocation);
                });


        loadData(loader);

        var result = processManager.initiateProviderRequest(createRequest());

        assertThat(latch.await(10000, MILLISECONDS)).isTrue();

        var transferProcess = store.find(result.getContent());

        assertThat(transferProcess).isNotNull();
        assertThat(transferProcess.getState()).isEqualTo(PROVISIONING.code());
    }

    @BeforeEach
    void setup(EdcExtension extension) {
        delegate = mock(Interceptor.class);
        var httpClient = testOkHttpClient().newBuilder().addInterceptor(delegate).build();

        extension.registerServiceMock(OkHttpClient.class, httpClient);
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
        extension.registerSystemExtension(ServiceExtension.class, new HttpProvisionerExtension(httpClient));
        extension.setConfiguration(PROVISIONER_CONFIG);
    }

    private void loadData(AssetLoader loader) {
        var asset = Asset.Builder.newInstance().id("1").build();
        var dataAddress = DataAddress.Builder.newInstance().type(TEST_DATA_TYPE).build();

        // load the asset
        loader.accept(asset, dataAddress);
    }

    private DataRequest createRequest() {
        return DataRequest.Builder.newInstance().destinationType("test").assetId("1").build();
    }

}
