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
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.junit.launcher.MockVault;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.functions.spi.flow.http.TransferFunctionInterceptorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static okhttp3.Protocol.HTTP_1_1;
import static org.eclipse.dataspaceconnector.transfer.functions.core.TransferFunctionsCoreServiceExtension.ENABLED_PROTOCOLS_KEY;
import static org.eclipse.dataspaceconnector.transfer.functions.core.TransferFunctionsCoreServiceExtension.TRANSFER_TYPE;

/**
 *
 */
@ExtendWith(EdcExtension.class)
public class TransferFunctionsCoreHttpTest {

    @Test
    void verifyLocalFlowControllerInvoked(TransferProcessManager processManager, TransferFunctionInterceptorRegistry registry)
            throws InterruptedException {

        final var latch = new CountDownLatch(1);
        var invoked = new AtomicBoolean();

        registry.registerHttpInterceptor(chain -> {
            invoked.set(true);
            latch.countDown();
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1).code(200)
                    .body(ResponseBody.create("", MediaType.get("application/json"))).message("")
                    .build();
        });

        var dataEntry = DataEntry.Builder.newInstance().id("test123").build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("ids")
                .destinationType("foo")
                .dataEntry(dataEntry)
                .managedResources(false)
                .dataDestination(DataAddress.Builder.newInstance().type("test-protocol1").build())
                .connectorId("test").build();

        processManager.initiateProviderRequest(dataRequest);

        latch.await(1, TimeUnit.MINUTES);
        Assertions.assertTrue(invoked.get());
    }

    @BeforeEach
    protected void before(EdcExtension extension) {
        System.setProperty(ENABLED_PROTOCOLS_KEY, "test-protocol1");
        System.setProperty(TRANSFER_TYPE, "http");

        // register a mock Vault
        extension.registerServiceMock(Vault.class, new MockVault());

        // register a wait strategy of 1ms to speed up the interval between transfer manager iterations
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
    }

    @AfterEach
    protected void after() {
        System.clearProperty(ENABLED_PROTOCOLS_KEY);
        System.clearProperty(TRANSFER_TYPE);
    }
    
}
