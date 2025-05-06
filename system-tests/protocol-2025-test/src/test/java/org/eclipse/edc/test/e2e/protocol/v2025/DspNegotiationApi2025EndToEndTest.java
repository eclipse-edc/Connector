/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Cofinity-X - make DSP versions pluggable
 *
 */

package org.eclipse.edc.test.e2e.protocol.v2025;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@EndToEndTest
public class DspNegotiationApi2025EndToEndTest {

    private static final int PROTOCOL_PORT = Ports.getFreePort();

    @RegisterExtension
    static RuntimeExtension runtime = new RuntimePerClassExtension(Dsp2025Runtime.createRuntimeWith(
            PROTOCOL_PORT,
            ":data-protocols:dsp:dsp-2025:dsp-negotiation-2025"
    ));

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionContextProvider.class)
    void shouldExposeVersion(String basePath, List<String> context) {
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
                .get(basePath + "/negotiations/" + id)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("'@type'", equalTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERM))
                .body("state", notNullValue())
                .body("consumerPid", notNullValue())
                .body("providerPid", notNullValue())
                .body("'@context'", equalTo(context));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionContextProvider.class)
    void shouldReturnError_whenNotFound(String basePath, List<String> context) {
        var id = UUID.randomUUID().toString();

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .get(basePath + "/negotiations/" + id)
                .then()
                .log().ifError()
                .statusCode(404)
                .contentType(JSON)
                .body("'@type'", equalTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM))
                .body("code", equalTo("404"))
                .body("reason[0]", equalTo("No negotiation with id %s found".formatted(id)))
                .body("'@context'", equalTo(context));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionContextProvider.class)
    void shouldReturnError_whenValidationFails(String basePath, List<String> context) {

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .body(createObjectBuilder()
                        .add(CONTEXT, createArrayBuilder(context))
                        .add(TYPE, "WrongType")
                        .add(DSPACE_PROPERTY_CONSUMER_PID_TERM, "any")
                        .add(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM, "any")
                        .add(DSPACE_PROPERTY_OFFER_TERM, createObjectBuilder()
                                .add("@type", ODRL_POLICY_TYPE_OFFER)
                                .add(ID, "offerId")
                                .add(ODRL_TARGET_ATTRIBUTE, "target")
                                .add(ODRL_ASSIGNER_ATTRIBUTE, "assigner")
                                .build())
                        .build())
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .post(basePath + "/negotiations/request")
                .then()
                .log().ifError()
                .statusCode(400)
                .contentType(JSON)
                .body("'@type'", equalTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM))
                .body("code", equalTo("400"))
                .body("reason[0]", equalTo("Bad request."))
                .body("'@context'", equalTo(context));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionContextProvider.class)
    void terminate_ShouldReturnError_whenMissingToken(String basePath, List<String> context) {
        var id = UUID.randomUUID().toString();

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .post(basePath + "/negotiations/" + id + "/termination")
                .then()
                .log().ifError()
                .statusCode(401)
                .contentType(JSON)
                .body("'@type'", equalTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM))
                .body("code", equalTo("401"))
                .body("reason[0]", equalTo("Unauthorized."))
                .body("'@context'", equalTo(context));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionContextProvider.class)
    void terminate_ShouldReturnError_whenValidationFails(String basePath, List<String> context) {
        var id = UUID.randomUUID().toString();

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .body(createObjectBuilder()
                        .add(CONTEXT, createArrayBuilder(context))
                        .add(TYPE, "FakeType")
                        .build())
                .post(basePath + "/negotiations/" + id + "/termination")

                .then()
                .log().ifError()
                .statusCode(400)
                .contentType(JSON)
                .body("'@type'", equalTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM))
                .body("code", equalTo("400"))
                .body("reason[0]", equalTo("Bad request."))
                .body("'@context'", equalTo(context));
    }

}
