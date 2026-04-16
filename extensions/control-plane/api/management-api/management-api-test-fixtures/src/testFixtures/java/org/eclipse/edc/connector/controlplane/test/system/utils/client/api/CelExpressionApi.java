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

import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.CelExpressionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static com.apicatalog.jsonld.lang.Keywords.ID;
import static io.restassured.http.ContentType.JSON;

/**
 * API client for CEL expression-related operations.
 */
public class CelExpressionApi {
    private final ManagementApiClientV5 connector;

    public CelExpressionApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Creates a CEL expression.
     *
     * @param expression the CEL expression to create
     * @return the ID of the created CEL expression
     */
    public String createExpression(CelExpressionDto expression) {
        return connector.baseManagementRequest(null, ParticipantPrincipal.ROLE_PROVISIONER)
                .contentType(JSON)
                .body(new WithContext<>(expression))
                .when()
                .post("/v5beta/celexpressions")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }


}
