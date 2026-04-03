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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractNegotiationDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ContractRequestDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.QuerySpectDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import java.util.Arrays;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * API client for contract negotiation-related operations.
 */
public class ContractNegotiationApi {
    private final ManagementApiClientV5 connector;

    public ContractNegotiationApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Initiates a contract negotiation in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param contractRequest      the contract request
     * @return the ID of the initiated contract negotiation
     */
    public String initContractNegotiation(String participantContextId, ContractRequestDto contractRequest) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(contractRequest))
                .when()
                .post("/v5alpha/participants/%s/contractnegotiations".formatted(participantContextId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    public String getState(String participantContextId, String negotiationId) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .when()
                .get("/v5alpha/participants/%s/contractnegotiations/%s/state".formatted(participantContextId, negotiationId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString("state");
    }

    public ContractNegotiationDto getNegotiation(String participantContextId, String negotiationId) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .when()
                .get("/v5alpha/participants/%s/contractnegotiations/%s".formatted(participantContextId, negotiationId))
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(ContractNegotiationDto.class);
    }

    public List<ContractNegotiationDto> search(String participantContextId, QuerySpectDto filter) {
        return Arrays.stream(connector.baseManagementRequest(participantContextId)
                        .contentType(JSON)
                        .body(new WithContext<>(filter))
                        .when()
                        .post("/v5alpha/participants/%s/contractnegotiations/request".formatted(participantContextId))
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .contentType(JSON)
                        .extract().as(ContractNegotiationDto[].class))
                .toList();
    }


}
