/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.transformer.JsonObjectToCallbackAddressTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractOfferTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToTerminateNegotiationCommandTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation.ContractRequestValidator;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation.TerminateNegotiationValidator;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3.ContractRequestSchema.CONTRACT_REQUEST_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3.TerminateNegotiationSchema.TERMINATE_NEGOTIATION_EXAMPLE;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class BaseContractNegotiationApiTest {
    protected final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToContractRequestTransformer());
        transformer.register(new JsonObjectToContractOfferTransformer());
        transformer.register(new JsonObjectToCallbackAddressTransformer());
        transformer.register(new JsonObjectToTerminateNegotiationCommandTransformer());
        ParticipantIdMapper participantIdMapper = mock();
        when(participantIdMapper.fromIri(any())).thenAnswer(a -> a.getArgument(0));
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper).forEach(transformer::register);
    }

    @Test
    void contractRequestExample() throws JsonProcessingException {
        var validator = ContractRequestValidator.instance();

        var jsonObject = objectMapper.readValue(CONTRACT_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        AbstractResultAssert.assertThat(expanded).isSucceeded()
                .satisfies(exp -> AbstractResultAssert.assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, ContractRequest.class))
                .satisfies(transformResult -> AbstractResultAssert.assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> assertThat(transformed.getProtocol()).isNotBlank()));
    }

    @Test
    void offerExample() throws JsonProcessingException {
        var validator = ContractRequestValidator.offerValidator(JsonObjectValidator.newValidator()).build();

        var jsonObject = objectMapper.readValue(ContractNegotiationApiV3.OfferSchema.OFFER_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        AbstractResultAssert.assertThat(expanded).isSucceeded()
                .satisfies(exp -> AbstractResultAssert.assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, Policy.class))
                .satisfies(transformResult -> AbstractResultAssert.assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> assertThat(transformed.getAssigner()).isNotBlank()));
    }

    @Test
    void terminateNegotiationExample() throws JsonProcessingException {
        var validator = TerminateNegotiationValidator.instance();

        var jsonObject = objectMapper.readValue(TERMINATE_NEGOTIATION_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        AbstractResultAssert.assertThat(expanded).isSucceeded()
                .satisfies(exp -> AbstractResultAssert.assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, TerminateNegotiationCommand.class))
                .satisfies(transformResult -> AbstractResultAssert.assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getEntityId()).isNotBlank();
                            assertThat(transformed.getReason()).isNotBlank();
                        }));
    }
}
