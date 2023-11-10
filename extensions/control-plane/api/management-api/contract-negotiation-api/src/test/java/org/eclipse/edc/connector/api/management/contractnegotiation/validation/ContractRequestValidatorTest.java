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

package org.eclipse.edc.connector.api.management.contractnegotiation.validation;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.OFFER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROTOCOL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROVIDER_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ContractRequestValidatorTest {

    private final Validator<JsonObject> validator = ContractRequestValidator.instance();

    @Test
    void shouldSuccess_whenObjectIsValid() {
        var input = Json.createObjectBuilder()
                .add(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS, value("http://connector-address"))
                .add(PROTOCOL, value("protocol"))
                .add(PROVIDER_ID, value("connector-id"))
                .add(OFFER, createArrayBuilder().add(createObjectBuilder()
                        .add(OFFER_ID, value("offerId"))
                        .add(ASSET_ID, value("offerId"))
                        .add(POLICY, createArrayBuilder().add(createObjectBuilder()))
                ))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryPropertiesAreMissing() {
        var input = Json.createObjectBuilder().build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(PROTOCOL))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(OFFER));
    }

    @Test
    void shouldFail_whenOfferMandatoryPropertiesAreMissing() {
        var input = Json.createObjectBuilder()
                .add(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS, value("http://connector-address"))
                .add(PROTOCOL, value("protocol"))
                .add(PROVIDER_ID, value("connector-id"))
                .add(OFFER, createArrayBuilder().add(createObjectBuilder()))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(OFFER + "/" + OFFER_ID))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(OFFER + "/" + ASSET_ID))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(OFFER + "/" + POLICY));
    }

    @Test
    void shouldSucceed_whenDeprecatedConnectorAddressIsUsed() {
        var input = Json.createObjectBuilder()
                .add(CONNECTOR_ADDRESS, value("http://connector-address"))
                .add(PROTOCOL, value("protocol"))
                .add(PROVIDER_ID, value("connector-id"))
                .add(OFFER, createArrayBuilder().add(createObjectBuilder()
                        .add(OFFER_ID, value("offerId"))
                        .add(ASSET_ID, value("offerId"))
                        .add(POLICY, createArrayBuilder().add(createObjectBuilder()))
                ))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
