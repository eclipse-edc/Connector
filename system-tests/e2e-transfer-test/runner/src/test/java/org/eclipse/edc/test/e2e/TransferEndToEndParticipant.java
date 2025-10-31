/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import io.restassured.common.mapper.TypeRef;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;
import static org.eclipse.edc.web.spi.configuration.ApiContext.PROTOCOL;

public class TransferEndToEndParticipant extends Participant {

    protected TransferEndToEndParticipant() {
        super();
    }

    public static TransferEndToEndParticipant forContext(ComponentRuntimeContext ctx) {
        var id = ctx.getConfig().getString("edc.participant.id");
        return TransferEndToEndParticipant.Builder.newInstance()
                .id(id)
                .name(ctx.getName())
                .managementUrl(ctx.getEndpoint(MANAGEMENT))
                .protocolUrl(ctx.getEndpoint(PROTOCOL))
                .build();
    }

    /**
     * Get the EDR from the EDR cache by transfer process id.
     *
     * @param transferProcessId The transfer process id
     * @return The cached {@link DataAddress}
     */
    public DataAddress getEdr(String transferProcessId) {
        var dataAddressRaw = baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v3/edrs/{id}/dataaddress", transferProcessId)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().body().as(new TypeRef<Map<String, Object>>() {
                });


        var builder = DataAddress.Builder.newInstance();
        dataAddressRaw.forEach(builder::property);
        return builder.build();
    }

    /**
     * Pull data from provider using EDR.
     *
     * @param edr           endpoint data reference
     * @param queryParams   query parameters
     * @param bodyAssertion assertion to be verified on the body
     */
    public void pullData(DataAddress edr, Map<String, String> queryParams, ThrowingConsumer<String> bodyAssertion) {
        var data = given()
                .baseUri(edr.getStringProperty("endpoint"))
                .header("Authorization", edr.getStringProperty("authorization"))
                .queryParams(queryParams)
                .when()
                .get()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(data).satisfies(bodyAssertion);
    }

    public void postResponse(DataAddress edr, ThrowingConsumer<String> bodyAssertion) {
        var data = given()
                .baseUri(edr.getStringProperty("responseChannel-endpoint"))
                .header("Authorization", edr.getStringProperty("responseChannel-authorization"))
                .when()
                .post()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(data).satisfies(bodyAssertion);
    }

    public static class Builder extends Participant.Builder<TransferEndToEndParticipant, Builder> {

        protected Builder() {
            super(new TransferEndToEndParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder managementUrl(LazySupplier<URI> managementUrl) {
            participant.controlPlaneManagement = managementUrl;
            return this;
        }

        public Builder protocolUrl(LazySupplier<URI> managementUrl) {
            participant.controlPlaneProtocol = managementUrl;
            return this;
        }

    }

}
