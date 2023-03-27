/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.api.transferprocess.model.TransferProcessFailStateDto;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
@ExtendWith(EdcExtension.class)
class TransferProcessControlApiControllerIntegrationTest {

    private final int port = getFreePort();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.control.port", String.valueOf(port),
                "web.http.control.path", "/"
        ));
        var registry = mock(RemoteMessageDispatcherRegistry.class);
        when(registry.send(any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        extension.registerServiceMock(RemoteMessageDispatcherRegistry.class, registry);
    }

    @Test
    void callTransferProcessHookWithComplete(TransferProcessStore store) {
        store.save(createTransferProcess());

        baseRequest()
                .contentType("application/json")
                .post("/transferprocess/{processId}/complete", "tp-id")
                .then()
                .body(is(""))
                .statusCode(is(204));


        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull()
                    .extracting(StatefulEntity::getState).isEqualTo(COMPLETED.code());
        });
    }

    @Test
    void callTransferProcessHookWithError(TransferProcessStore store) {
        store.save(createTransferProcess());

        var rq = TransferProcessFailStateDto.Builder.newInstance()
                .errorMessage("error")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/transferprocess/{processId}/fail", "tp-id")
                .then()
                .body(is(""))
                .statusCode(is(204));


        await().untilAsserted(() -> {
            var transferProcess = store.find("tp-id");
            assertThat(transferProcess).isNotNull().satisfies((process) -> {
                assertThat(process.getState()).isEqualTo(TERMINATED.code());
                assertThat(process.getErrorDetail()).isEqualTo("error");
            });
        });
    }

    @Test
    void callTransferProcessHookWithErrorFailWithNoErrorMessageBody() {
        baseRequest()
                .body("{}")
                .contentType("application/json")
                .post("/transferprocess/{processId}/fail", "tp-id")
                .then()
                .statusCode(is(400));

    }

    @Test
    void callTransferProcessHookWithErrorFailWithNoBody() {
        baseRequest()
                .contentType("application/json")
                .post("/transferprocess/{processId}/fail", "tp-id")
                .then()
                .statusCode(is(400));

    }

    @Test
    void callFailTransferProcessHook_notFound() {
        var rq = TransferProcessFailStateDto.Builder.newInstance()
                .errorMessage("error")
                .build();
        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/transferprocess/nonExistingId/fail")
                .then()
                .statusCode(404);
    }

    @Test
    void callCompleteTransferProcessHook_notFound() {
        baseRequest()
                .contentType("application/json")
                .post("/transferprocess/nonExistingId/complete")
                .then()
                .statusCode(404);
    }

    @Test
    void callCompleteTransferProcessHook_invalidState(TransferProcessStore store) {
        store.save(createTransferProcessBuilder().state(INITIAL.code()).build());

        var rq = TransferProcessFailStateDto.Builder.newInstance()
                .errorMessage("error")
                .build();
        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/transferprocess/{processId}/complete", "tp-id")
                .then()
                .statusCode(409);
    }

    private TransferProcess createTransferProcess() {
        return createTransferProcessBuilder().build();
    }

    private TransferProcess.Builder createTransferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .id("tp-id")
                .state(TransferProcessStates.STARTED.code())
                .type(TransferProcess.Type.PROVIDER)
                .correlationId("testCorrelationId")
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType("file")
                        .protocol("protocol")
                        .connectorAddress("http://an/address")
                        .build());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }

}
