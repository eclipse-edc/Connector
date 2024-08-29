/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class TransferProcessQueryValidatorTest {

    private final QueryValidator queryValidator = TransferProcessQueryValidatorFactory.createQueryValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "deprovisionedResources.provisionedResourceId",
            "provisionedResourceSet.resources.resourceDefinitionId",
            "provisionedResourceSet.resources.hasToken",
            "privateProperties.someKey", // path element with privateProperties and key
            "privateProperties." + "'someKey'", // path element with privateProperties and 'key'
            "privateProperties." + EDC_NAMESPACE + "someKey", // path element with privateProperties and edc_namespace key
            "type",
    })
    void validate_shouldSucceed(String key) {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion(key, "=", "someval"))
                .build();

        var result = queryValidator.validate(query);
        assertThat(result).isSucceeded();
    }

    @Test
    public void validate_shouldFail_wrongCase() {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("provisionedResourceSet.resources.hastoken", "=", "true"))
                .build();

        var result = queryValidator.validate(query);
        assertThat(result).isFailed();
    }

    @Test
    public void validate_shouldFail_wrongProperty() {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("resourceManifest.definitions.notexist", "=", "foobar"))
                .build();

        var result = queryValidator.validate(query);
        assertThat(result).isFailed();
    }

    @Test
    public void validate_shouldFail_withMap() {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("contentDataAddress.properties[*].someKey", "=", "someval"))
                .build();

        var result = queryValidator.validate(query);
        assertThat(result).isFailed();
    }
}
