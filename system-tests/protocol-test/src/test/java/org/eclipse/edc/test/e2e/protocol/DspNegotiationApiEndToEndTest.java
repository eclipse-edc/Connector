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

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.protocol.dsp.version.DspVersions.V_2024_1;

@EndToEndTest
public class DspNegotiationApiEndToEndTest {

    private static final int PROTOCOL_PORT = TestUtils.getFreePort();

    @RegisterExtension
    static EdcRuntimeExtension runtime = new EdcRuntimeExtension(
            "runtime",
            Map.of(
                    "web.http.protocol.path", "/protocol",
                    "web.http.protocol.port", String.valueOf(PROTOCOL_PORT)
            ),
            ":data-protocols:dsp:dsp-negotiation:dsp-negotiation-api",
            ":data-protocols:dsp:dsp-negotiation:dsp-negotiation-transform",
            ":data-protocols:dsp:dsp-api-configuration",
            ":data-protocols:dsp:dsp-http-core",
            ":extensions:common:iam:iam-mock",
            ":core:control-plane:control-plane-aggregate-services",
            ":core:control-plane:control-plane-core",
            ":extensions:common:http"
    );

    @Test
    void shouldExposeVersion2024_1() {
        var id = UUID.randomUUID().toString();
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(id).counterPartyId("any").counterPartyAddress("any").protocol("any").state(REQUESTED.code())
                .correlationId(UUID.randomUUID().toString())
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(UUID.randomUUID().toString()).assetId(UUID.randomUUID().toString())
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
        runtime.getService(ContractNegotiationStore.class).save(negotiation);

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .get("/2024/1/negotiations/" + id)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON);

        assertThat(runtime.getContext().getService(ProtocolVersionRegistry.class).getAll().protocolVersions())
                .contains(V_2024_1);
    }

}
