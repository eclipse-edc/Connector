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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * API client for contract definition-related operations.
 */
public class ContractDefApi {

    private final ManagementApiClientV5 connector;

    public ContractDefApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Creates a contract definition in the specified participant context.
     *
     * @param participantContextId  the participant context ID
     * @param contractDefinitionDto the contract definition to create
     * @return the ID of the created contract definition
     */
    public String createContractDefinition(String participantContextId, ContractDefinitionDto contractDefinitionDto) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(contractDefinitionDto))
                .when()
                .post("/v5alpha/participants/%s/contractdefinitions".formatted(participantContextId))
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }
}
