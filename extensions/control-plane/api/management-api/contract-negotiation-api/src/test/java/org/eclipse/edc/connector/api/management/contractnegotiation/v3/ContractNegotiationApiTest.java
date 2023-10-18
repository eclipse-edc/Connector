/*
 *  Copyright (c) 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Negotiation API enhancement
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.transformer.JsonObjectToCallbackAddressTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectToTerminateNegotiationCommandTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.v3.transform.ContractRequestDtoToContractRequestTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.v3.transform.JsonObjectToContractRequestDtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.validation.ContractRequestValidator;
import org.eclipse.edc.connector.api.management.contractnegotiation.validation.TerminateNegotiationValidator;
import org.eclipse.edc.connector.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.core.transform.transformer.OdrlTransformersFactory;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.api.management.contractnegotiation.ContractNegotiationApi.NegotiationStateSchema.NEGOTIATION_STATE_EXAMPLE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.ContractNegotiationApi.TerminateNegotiationSchema.TERMINATE_NEGOTIATION_EXAMPLE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationState.NEGOTIATION_STATE_STATE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationState.NEGOTIATION_STATE_TYPE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.ContractNegotiationApi.ContractRequestSchema.CONTRACT_REQUEST_EXAMPLE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;

class ContractNegotiationApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToContractRequestDtoTransformer());
        transformer.register(new ContractRequestDtoToContractRequestTransformer());
        transformer.register(new JsonObjectToCallbackAddressTransformer());
        transformer.register(new JsonObjectToTerminateNegotiationCommandTransformer());
        OdrlTransformersFactory.jsonObjectToOdrlTransformers().forEach(transformer::register);
    }

    @Test
    void contractRequestExample() throws JsonProcessingException {
        var validator = ContractRequestValidator.instance();

        var jsonObject = objectMapper.readValue(CONTRACT_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, ContractRequest.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getProviderId()).isNotBlank();
                        }));
    }

    @Test
    void terminateNegotiationExample() throws JsonProcessingException {
        var validator = TerminateNegotiationValidator.instance();

        var jsonObject = objectMapper.readValue(TERMINATE_NEGOTIATION_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        assertThat(expanded).isSucceeded()
                .satisfies(exp -> assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, TerminateNegotiationCommand.class))
                .satisfies(transformResult -> assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getEntityId()).isNotBlank();
                            assertThat(transformed.getReason()).isNotBlank();
                        }));
    }

    @Test
    void negotiationStateExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(NEGOTIATION_STATE_EXAMPLE, JsonObject.class);
        var expanded = jsonLd.expand(jsonObject);

        assertThat(expanded).isSucceeded().satisfies(content -> {
            assertThat(content.getJsonArray(TYPE).getString(0)).isEqualTo(NEGOTIATION_STATE_TYPE);
            assertThat(content.getJsonArray(NEGOTIATION_STATE_STATE).getJsonObject(0).getString(VALUE)).isNotBlank();
        });
    }

}
