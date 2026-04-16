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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DataPlaneRegistrationDto;

import static io.restassured.http.ContentType.JSON;

/**
 * API client for transfer-related operations.
 */
public class DataPlaneApi {
    private final ManagementApiClientV5 connector;

    public DataPlaneApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Registers a new data plane with the control plane using the provided registration details.
     *
     * @param participantContextId  the participant context ID
     * @param dataPlaneRegistration the dataplane registration details
     */
    public void registerDataPlane(String participantContextId, DataPlaneRegistrationDto dataPlaneRegistration) {
        connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(dataPlaneRegistration)
                .when()
                .put("/v5beta/participants/%s/dataplanes".formatted(participantContextId))
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

}
