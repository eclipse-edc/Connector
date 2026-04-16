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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * API client for policy definition-related operations.
 */
public class PolicyDefApi {

    private final ManagementApiClientV5 connector;

    public PolicyDefApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Creates a policy definition in the specified participant context.
     *
     * @param participantContextId the participant context ID
     * @param policyDef            the policy definition to create
     * @return the ID of the created policy definition
     */
    public String createPolicyDefinition(String participantContextId, PolicyDefinitionDto policyDef) {
        return connector.baseManagementRequest(participantContextId)
                .contentType(JSON)
                .body(new WithContext<>(policyDef))
                .when()
                .post("/v5beta/participants/%s/policydefinitions".formatted(participantContextId))
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }
}
