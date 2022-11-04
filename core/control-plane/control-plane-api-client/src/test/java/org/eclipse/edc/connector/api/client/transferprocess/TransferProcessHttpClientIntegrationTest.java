/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.client.transferprocess;

import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.api.ControlPlaneApiExtension.DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.ERROR;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class TransferProcessHttpClientIntegrationTest {

    private final String authKey = "123456";
    private final int port = getFreePort();

    private TransferService service;

    @BeforeEach
    void setUp(EdcExtension extension) {

        service = mock(TransferService.class);
        when(service.canHandle(any())).thenReturn(true);


        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.controlplane.port", String.valueOf(port),
                "web.http.controlplane.path", DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH
        ));

        extension.registerSystemExtension(ServiceExtension.class, new TransferServiceMockExtension(service));
    }

    @Test
    void shouldCallTransferProcessApiWithComplete(TransferProcessStore store, DataPlaneManager manager, ControlPlaneApiUrl callbackUrl) {
        when(service.transfer(any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success()));
        var id = "tp-id";
        store.create(createTransferProcess(id));
        manager.initiateTransfer(createDataFlowRequest(id, callbackUrl.get()));

        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull()
                    .extracting(StatefulEntity::getState).isEqualTo(COMPLETED.code());
        });
    }


    @Test
    void shouldCallTransferProcessApiWithFailed(TransferProcessStore store, DataPlaneManager manager, ControlPlaneApiUrl callbackUrl) {
        when(service.transfer(any())).thenReturn(CompletableFuture.completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, "error")));
        var id = "tp-id";
        store.create(createTransferProcess(id));
        manager.initiateTransfer(createDataFlowRequest(id, callbackUrl.get()));

        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull().satisfies(process -> {
                assertThat(process.getState()).isEqualTo(ERROR.code());
                assertThat(process.getErrorDetail()).isEqualTo("error");
            });
        });
    }

    @Test
    void shouldCallTransferProcessApiWithException(TransferProcessStore store, DataPlaneManager manager, ControlPlaneApiUrl callbackUrl) {
        when(service.transfer(any())).thenReturn(CompletableFuture.failedFuture(new EdcException("error")));
        var id = "tp-id";
        store.create(createTransferProcess(id));
        manager.initiateTransfer(createDataFlowRequest(id, callbackUrl.get()));

        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull().satisfies(process -> {
                assertThat(process.getState()).isEqualTo(ERROR.code());
                assertThat(process.getErrorDetail()).isEqualTo("error");
            });
        });
    }

    private TransferProcess createTransferProcess(String id) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .state(TransferProcessStates.IN_PROGRESS.code())
                .type(TransferProcess.Type.PROVIDER)
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType("file")
                        .build())
                .build();
    }

    private DataFlowRequest createDataFlowRequest(String processId, URL callbackAddress) {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(processId)
                .callbackAddress(callbackAddress)
                .sourceDataAddress(DataAddress.Builder.newInstance().type("file").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("file").build())
                .build();
    }

    private static class TransferServiceMockExtension implements ServiceExtension {

        private final TransferService transferService;
        @Inject
        private TransferServiceRegistry registry;

        private TransferServiceMockExtension(TransferService transferService) {
            this.transferService = transferService;
        }

        @Override
        public void initialize(ServiceExtensionContext context) {
            registry.registerTransferService(transferService);
        }
    }
}
