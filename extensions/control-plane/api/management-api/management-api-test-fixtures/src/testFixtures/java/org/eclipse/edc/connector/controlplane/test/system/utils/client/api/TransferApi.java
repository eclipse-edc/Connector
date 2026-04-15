/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api;

import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.SuspendTransferDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.TerminateTransferDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.TransferProcessDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.TransferRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * API client for transfer-related operations.
 */
public class TransferApi {
    private final ManagementApiClientV5 connector;

    public TransferApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Initiates a transfer process in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param transferRequest      the transfer request
     * @return the ID of the initiated transfer process
     */
    public String initTransfer(String participantContextId, TransferRequestDto transferRequest) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(transferRequest))
                .when()
                .post("/v5beta/participants/%s/transferprocesses".formatted(participantContextId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Retrieves the state of a transfer process in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param transferId           the transfer process ID
     * @return the state of the transfer process
     */
    public String getState(String participantContextId, String transferId) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .when()
                .get("/v5beta/participants/%s/transferprocesses/%s/state".formatted(participantContextId, transferId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString("state");
    }

    /**
     * Retrieves a transfer process in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param transferId           the transfer process ID
     * @return the transfer process
     */
    public TransferProcessDto getTransferProcess(String participantContextId, String transferId) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .when()
                .get("/v5beta/participants/%s/transferprocesses/%s".formatted(participantContextId, transferId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(TransferProcessDto.class);
    }

    /**
     * Suspends a transfer process in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param transferId           the transfer process ID
     * @param reason               the reason for suspension
     */
    public void suspendTransfer(String participantContextId, String transferId, String reason) {
        var suspendTransferDto = new SuspendTransferDto(reason);
        connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(suspendTransferDto))
                .when()
                .post("/v5beta/participants/%s/transferprocesses/%s/suspend".formatted(participantContextId, transferId))
                .then()
                .log().ifValidationFails()
                .statusCode(204);

    }

    /**
     * Suspends a transfer process in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param transferId           the transfer process ID
     * @param reason               the reason for suspension
     */
    public void terminateTransfer(String participantContextId, String transferId, String reason) {
        var terminateTransferDto = new TerminateTransferDto(reason);
        connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(terminateTransferDto))
                .when()
                .post("/v5beta/participants/%s/transferprocesses/%s/terminate".formatted(participantContextId, transferId))
                .then()
                .log().ifValidationFails()
                .statusCode(204);

    }

    /**
     * Resumes a suspended transfer process in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param transferId           the transfer process ID
     */
    public void resumeTransfer(String participantContextId, String transferId) {
        connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .when()
                .post("/v5beta/participants/%s/transferprocesses/%s/resume".formatted(participantContextId, transferId))
                .then()
                .log().ifValidationFails()
                .statusCode(204);

    }
}
