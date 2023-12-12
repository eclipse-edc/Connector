/*
 *  Copyright (c) 2023 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       SAP - initial API and implementation
 *
 */

package org.eclipse.edc.connector.service.contractdefinition;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionQueryValidatorTest {

    private final ContractDefinitionQueryValidator validator = new ContractDefinitionQueryValidator();


    @ParameterizedTest
    @ValueSource(strings = {

            ".someValue", //leading slash
            "<42ValidValues" //leading number
    })
    void validate_invalidProperty(String key) {
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion(key, "=", "something")))
                .build();

        assertThat(validator.validate(query).failed()).isTrue();
    }

}