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

package org.eclipse.edc.test.e2e.protocol;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;
import static org.hamcrest.CoreMatchers.equalTo;

@EndToEndTest
public class DspTransferApiEndToEndTest {

    private static final int PROTOCOL_PORT = Ports.getFreePort();

    @RegisterExtension
    static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime(
            "runtime",
            Map.of(
                    "web.http.protocol.path", "/protocol",
                    "web.http.protocol.port", String.valueOf(PROTOCOL_PORT)
            ),
            ":data-protocols:dsp:dsp-transfer-process:dsp-transfer-process-http-api",
            ":data-protocols:dsp:dsp-transfer-process:dsp-transfer-process-transform",
            ":data-protocols:dsp:dsp-http-api-configuration",
            ":data-protocols:dsp:dsp-http-core",
            ":extensions:common:iam:iam-mock",
            ":core:control-plane:control-plane-aggregate-services",
            ":core:control-plane:control-plane-core",
            ":extensions:common:http"
    ));

    private static ContractNegotiation createNegotiationWithAgreement(String contractId) {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString()).counterPartyId("any").counterPartyAddress("any").protocol("any").state(ContractNegotiationStates.REQUESTED.code())
                .correlationId(UUID.randomUUID().toString())
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(UUID.randomUUID().toString()).assetId(UUID.randomUUID().toString())
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .id(contractId)
                        .providerId("any")
                        .consumerId("any")
                        .assetId("any")
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
    }

    @Test
    void shouldExposeVersion2024_1() {
        var id = UUID.randomUUID().toString();
        var contractId = UUID.randomUUID().toString();
        var transfer = TransferProcess.Builder.newInstance()
                .id(id)
                .contractId(contractId)
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .state(REQUESTED.code())
                .build();
        runtime.getService(TransferProcessStore.class).save(transfer);
        runtime.getService(ContractNegotiationStore.class).save(createNegotiationWithAgreement(contractId));

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .get("/2024/1/transfers/" + id)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON);

        assertThat(runtime.getService(ProtocolVersionRegistry.class).getAll().protocolVersions())
                .contains(V_2024_1);
    }

    @Test
    void shouldReturnError_whenNotFound() {
        var id = UUID.randomUUID().toString();

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .get("/transfers/" + id)
                .then()
                .log().ifError()
                .statusCode(404)
                .contentType(JSON)
                .body("'@type'", equalTo("dspace:TransferError"))
                .body("'dspace:code'", equalTo("404"))
                .body("'dspace:reason'", equalTo("No transfer process with id %s found".formatted(id)))
                .body("'@context'.dspace", equalTo(DSPACE_SCHEMA));

    }

    @Test
    void shouldReturnError_whenTokenIsMissing() {
        var id = UUID.randomUUID().toString();

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .get("/transfers/" + id)
                .then()
                .log().ifError()
                .statusCode(401)
                .contentType(JSON)
                .body("'@type'", equalTo("dspace:TransferError"))
                .body("'dspace:code'", equalTo("401"))
                .body("'dspace:reason'", equalTo("Unauthorized."))
                .body("'@context'.dspace", equalTo(DSPACE_SCHEMA));

    }

}
