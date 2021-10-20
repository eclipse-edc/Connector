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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.awaitility.Awaitility;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.transfer.functions.core.TransferFunctionsCoreServiceExtension.ENABLED_PROTOCOLS_KEY;

/**
 * Verifies the HTTP flow controller works.
 */
@ExtendWith(EdcExtension.class)
public class TransferFunctionsCoreHttpTest {

    private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9090));

    @Test
    void verifyHttpFlowControllerInvoked(TransferProcessManager processManager, TransferProcessStore processStore) throws InterruptedException {
        wireMockServer.stubFor(post("/transfer").willReturn(ok()));

        var dataEntry = DataEntry.Builder.newInstance().id("test123").build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("ids")
                .destinationType("foo")
                .dataEntry(dataEntry)
                .managedResources(false)
                .dataDestination(DataAddress.Builder.newInstance().type("test-protocol1").build())
                .connectorId("test").build();

        TransferInitiateResponse response = processManager.initiateProviderRequest(dataRequest);

        await().untilAsserted(() -> {
            assertThat(response.getStatus()).isEqualTo(ResponseStatus.OK);
            TransferProcess transferProcess = processStore.find(response.getId());
            assertThat(transferProcess.getState()).isEqualTo(TransferProcessStates.IN_PROGRESS.code());
        });
    }

    @BeforeEach
    protected void before(EdcExtension extension) {
        wireMockServer.start();
        System.setProperty(ENABLED_PROTOCOLS_KEY, "test-protocol1");

        // register a wait strategy of 1ms to speed up the interval between transfer manager iterations
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
    }

    @AfterEach
    protected void after() {
        wireMockServer.stop();
        System.clearProperty(ENABLED_PROTOCOLS_KEY);
    }

}
