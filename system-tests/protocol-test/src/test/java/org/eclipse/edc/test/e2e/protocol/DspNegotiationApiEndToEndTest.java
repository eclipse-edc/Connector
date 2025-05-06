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
 *       Cofinity-X - refactor DSP module structure to make versions pluggable
 *
 */

package org.eclipse.edc.test.e2e.protocol;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@EndToEndTest
public class DspNegotiationApiEndToEndTest {

    private static final int PROTOCOL_PORT = Ports.getFreePort();

    @RegisterExtension
    static RuntimeExtension runtime = new RuntimePerClassExtension(DspRuntime.createRuntimeWith(
            PROTOCOL_PORT,
            ":data-protocols:dsp:dsp-08:dsp-negotiation-08",
            ":data-protocols:dsp:dsp-2024:dsp-negotiation-2024"
    ));

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionProvider.class)
    void shouldExposeVersion(String basePath, JsonLdNamespace namespace) {
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
                .body("'@type'", equalTo("dspace:ContractNegotiation"))
                .body("'dspace:state'", notNullValue())
                .body("'dspace:consumerPid'", notNullValue())
                .body("'dspace:providerPid'", notNullValue())
                .body("'@context'.dspace", equalTo(namespace.namespace()));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionProvider.class)
    void shouldReturnError_whenNotFound(String basePath, JsonLdNamespace namespace) {
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
                .body("'@type'", equalTo("dspace:ContractNegotiationError"))
                .body("'dspace:code'", equalTo("404"))
                .body("'dspace:reason'", equalTo("No negotiation with id %s found".formatted(id)))
                .body("'@context'.dspace", equalTo(namespace.namespace()));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionProvider.class)
    void shouldReturnError_whenValidationFails(String basePath, JsonLdNamespace namespace) {

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .body(createObjectBuilder()
                        .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                        .add(TYPE, "WrongType")
                        .add(namespace.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "any")
                        .add(namespace.toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), "any")
                        .add(namespace.toIri(DSPACE_PROPERTY_OFFER_TERM), createObjectBuilder()
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
                .body("'@type'", equalTo("dspace:ContractNegotiationError"))
                .body("'dspace:code'", equalTo("400"))
                .body("'dspace:reason'", equalTo("Bad request."))
                .body("'@context'.dspace", equalTo(namespace.namespace()));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionProvider.class)
    void terminate_ShouldReturnError_whenMissingToken(String basePath, JsonLdNamespace namespace) {
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
                .body("'@type'", equalTo("dspace:ContractNegotiationError"))
                .body("'dspace:code'", equalTo("401"))
                .body("'dspace:reason'", equalTo("Unauthorized."))
                .body("'@context'.dspace", equalTo(namespace.namespace()));
    }

    @ParameterizedTest
    @ArgumentsSource(ProtocolVersionProvider.class)
    void terminate_ShouldReturnError_whenValidationFails(String basePath, JsonLdNamespace namespace) {
        var id = UUID.randomUUID().toString();

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .contentType(JSON)
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .body(createObjectBuilder()
                        .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                        .add(TYPE, "FakeType")
                        .build())
                .post(basePath + "/negotiations/" + id + "/termination")

                .then()
                .log().ifError()
                .statusCode(400)
                .contentType(JSON)
                .body("'@type'", equalTo("dspace:ContractNegotiationError"))
                .body("'dspace:code'", equalTo("400"))
                .body("'dspace:reason'", equalTo("Bad request."))
                .body("'@context'.dspace", equalTo(namespace.namespace()));
    }

}
