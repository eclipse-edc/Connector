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

package org.eclipse.edc.connector.controlplane.api.management.catalog.validation;

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
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_FIELD;
import static org.mockito.Mockito.mock;

class CatalogRequestValidatorTest {

    private final Validator<JsonObject> validator = CatalogRequestValidator.instance(mock());

    @Test
    void shouldSucceed_whenInputIsValid() {
        var input = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, value("anId"))
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, value("http://any"))
                .add(CATALOG_REQUEST_PROTOCOL, value("protocol"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryFieldsAreMissing() {
        var input = Json.createObjectBuilder().build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .hasSize(3)
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(CATALOG_REQUEST_COUNTER_PARTY_ID))
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS))
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(CATALOG_REQUEST_PROTOCOL));
    }

    @Test
    void shouldFail_whenOptionalQuerySpecIsInvalid() {
        var input = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, value("anId"))
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, value("http://any"))
                .add(CATALOG_REQUEST_PROTOCOL, value("protocol"))
                .add(CATALOG_REQUEST_QUERY_SPEC, createArrayBuilder().add(createObjectBuilder()
                        .add(EDC_QUERY_SPEC_SORT_FIELD, value(" "))))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .hasSize(1)
                .anySatisfy(v -> assertThat(v.path()).startsWith(CATALOG_REQUEST_QUERY_SPEC));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
