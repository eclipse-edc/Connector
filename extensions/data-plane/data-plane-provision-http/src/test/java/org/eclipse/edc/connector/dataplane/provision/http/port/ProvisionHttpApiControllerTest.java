/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.provision.http.port;

import io.restassured.http.ContentType;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProvisionHttpApiControllerTest extends RestControllerTestBase {

    private final DataPlaneManager dataPlaneManager = mock();

    @Test
    void provision_shouldCallProvisionOnManager() {
        when(dataPlaneManager.resourceProvisioned(any())).thenReturn(ServiceResult.success());
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/flowId/resourceId/provision")
                .then()
                .statusCode(204);

        var captor = ArgumentCaptor.forClass(ProvisionedResource.class);
        verify(dataPlaneManager).resourceProvisioned(captor.capture());
        assertThat(captor.getValue()).satisfies(provisioned -> {
            assertThat(provisioned.getId()).isEqualTo("resourceId");
            assertThat(provisioned.getFlowId()).isEqualTo("flowId");
            assertThat(provisioned.isPending()).isFalse();
        });
    }

    @Test
    void deprovision_shouldCallProvisionOnManager() {
        when(dataPlaneManager.resourceDeprovisioned(any())).thenReturn(ServiceResult.success());
        given()
                .port(port)
                .post("/flowId/resourceId/deprovision")
                .then()
                .statusCode(204);

        var captor = ArgumentCaptor.forClass(DeprovisionedResource.class);
        verify(dataPlaneManager).resourceDeprovisioned(captor.capture());
        assertThat(captor.getValue()).satisfies(deprovisioned -> {
            assertThat(deprovisioned.getId()).isEqualTo("resourceId");
            assertThat(deprovisioned.getFlowId()).isEqualTo("flowId");
            assertThat(deprovisioned.isPending()).isFalse();
        });
    }

    @Override
    protected Object controller() {
        return new ProvisionHttpApiController(dataPlaneManager);
    }
}
