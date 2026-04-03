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
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.ParticipantContextDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.WithContext;

import static io.restassured.http.ContentType.JSON;

public class ParticipantContextApi {
    private final ManagementApiClientV5 connector;

    public ParticipantContextApi(ManagementApiClientV5 connector) {
        this.connector = connector;
    }

    /**
     * Creates a participant context.
     *
     * @param participantContextDto the participant context to create
     */
    public void createParticipant(ParticipantContextDto participantContextDto) {
        connector.baseManagementRequest(null, ParticipantPrincipal.ROLE_PROVISIONER)
                .contentType(JSON)
                .body(new WithContext<>(participantContextDto))
                .when()
                .post("/v5alpha/participants")
                .then()
                .log().ifError()
                .statusCode(200);
    }


}
