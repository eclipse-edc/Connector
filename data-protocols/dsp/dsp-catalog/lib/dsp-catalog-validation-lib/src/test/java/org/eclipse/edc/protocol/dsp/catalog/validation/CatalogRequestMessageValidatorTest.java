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

package org.eclipse.edc.protocol.dsp.catalog.validation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class CatalogRequestMessageValidatorTest {

    private final Validator<JsonObject> validator = CatalogRequestMessageValidator.instance(mock());

    @Test
    void shouldSucceed_whenObjectIsValid() {
        var input = Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_IRI))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryFieldsAreMissing() {
        var input = Json.createObjectBuilder()
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .hasSize(1)
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(TYPE));
    }
}
