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

package org.eclipse.edc.connector.api.client.transferprocess;

import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
@ComponentTest
public class TransferProcessHttpClientIntegrationTest {

    private final int port = getFreePort();
    private final TransferService service = mock();

    @BeforeEach
    void setUp(EdcExtension extension) {
        when(service.canHandle(any())).thenReturn(true);

        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.control.port", String.valueOf(port),
                "web.http.control.path", "/control",
                "edc.core.retry.retries.max", "0",
                "edc.dataplane.send.retry.limit", "0"
        ));

        extension.registerSystemExtension(ServiceExtension.class, new TransferServiceMockExtension(service));
        extension.registerServiceMock(ProtocolWebhook.class, mock());
        var registry = mock(RemoteMessageDispatcherRegistry.class);
        when(registry.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
        extension.registerServiceMock(RemoteMessageDispatcherRegistry.class, registry);
    }

    @Test
    void shouldCallTransferProcessApiWithComplete(TransferProcessStore store, DataPlaneManager manager, ControlApiUrl callbackUrl) {
        when(service.transfer(any())).thenReturn(completedFuture(StreamResult.success()));
        var id = "tp-id";
        store.save(createTransferProcess(id));
        var dataFlowRequest = createDataFlowRequest(id, callbackUrl.get());

        manager.initiate(dataFlowRequest);

        await().untilAsserted(() -> {
            var transferProcess = store.findById("tp-id");
            assertThat(transferProcess).isNotNull()
                    .extracting(StatefulEntity::getState).asInstanceOf(INTEGER).isGreaterThanOrEqualTo(COMPLETED.code());
        });
    }

    @Test
    void shouldCallTransferProcessApiWithFailed(TransferProcessStore store, DataPlaneManager manager, ControlApiUrl callbackUrl) {
        when(service.transfer(any())).thenReturn(completedFuture(StreamResult.error("error")));
        var id = "tp-id";
        store.save(createTransferProcess(id));
        var dataFlowRequest = createDataFlowRequest(id, callbackUrl.get());

        manager.initiate(dataFlowRequest);

        await().untilAsserted(() -> {
            var transferProcess = store.findById("tp-id");
            assertThat(transferProcess).isNotNull().satisfies(process -> {
                assertThat(process.getState()).isGreaterThanOrEqualTo(TERMINATED.code());
                assertThat(process.getErrorDetail()).isEqualTo("error");
            });
        });
    }

    @Test
    void shouldCallTransferProcessApiWithException(TransferProcessStore store, DataPlaneManager manager, ControlApiUrl callbackUrl) {
        when(service.transfer(any())).thenReturn(failedFuture(new EdcException("error")));
        var id = "tp-id";
        store.save(createTransferProcess(id));
        var dataFlowRequest = createDataFlowRequest(id, callbackUrl.get());

        manager.initiate(dataFlowRequest);

        await().untilAsserted(() -> {
            var transferProcess = store.findById("tp-id");
            assertThat(transferProcess).isNotNull().satisfies(process -> {
                assertThat(process.getState()).isGreaterThanOrEqualTo(TERMINATED.code());
                assertThat(process.getErrorDetail()).isEqualTo("error");
            });
        });
    }

    private TransferProcess createTransferProcess(String id) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .state(TransferProcessStates.STARTED.code())
                .type(TransferProcess.Type.PROVIDER)
                .dataRequest(DataRequest.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .destinationType("file")
                        .protocol("any")
                        .connectorAddress("http://an/address")
                        .build())
                .build();
    }

    private DataFlowRequest createDataFlowRequest(String processId, URI callbackAddress) {
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

        @Inject
        private DataFlowManager dataFlowManager;

        private TransferServiceMockExtension(TransferService transferService) {
            this.transferService = transferService;
        }

        @Override
        public void initialize(ServiceExtensionContext context) {
            registry.registerTransferService(transferService);
            DataFlowController controller = mock();
            when(controller.canHandle(any())).thenReturn(true);
            when(controller.terminate(any())).thenReturn(StatusResult.success());
            dataFlowManager.register(controller);
        }
    }

}
