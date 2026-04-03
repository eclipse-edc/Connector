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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AssetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * API client for asset-related operations.
 */
public class AssetsApi {
    private final ManagementApiClientV5 connector;

    public AssetsApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Creates an asset in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param asset                the asset to create
     * @return the ID of the created asset
     */
    public String createAsset(String participantContextId, AssetDto asset) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(asset))
                .when()
                .post("/v5alpha/participants/%s/assets".formatted(participantContextId))
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }


}
