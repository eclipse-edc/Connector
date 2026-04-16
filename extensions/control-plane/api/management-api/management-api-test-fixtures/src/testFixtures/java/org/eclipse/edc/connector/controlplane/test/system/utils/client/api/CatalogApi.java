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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DatasetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DatasetRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static io.restassured.http.ContentType.JSON;

/**
 * API client for catalog-related operations.
 */
public class CatalogApi {
    private final ManagementApiClientV5 connector;

    public CatalogApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Requests a dataset from the catalog in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param datasetRequest       the dataset request
     * @return the requested dataset
     */
    public DatasetDto getDataset(String participantContextId, DatasetRequestDto datasetRequest) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(datasetRequest))
                .when()
                .post("/v5beta/participants/%s/catalog/dataset/request".formatted(participantContextId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(DatasetDto.class);
    }


}
