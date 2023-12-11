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

package org.eclipse.edc.connector.service.policydefinition;

import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDefinitionQueryValidatorTest {

    private final PolicyDefinitionQueryValidator validator = new PolicyDefinitionQueryValidator(Map.of(
            Constraint.class, List.of(MultiplicityConstraint.class, AtomicConstraint.class),
            MultiplicityConstraint.class, List.of(AndConstraint.class, OrConstraint.class, XoneConstraint.class),
            Expression.class, List.of(LiteralExpression.class)
    ));

    @ParameterizedTest
    @ValueSource(strings = {
            "someCustomVal",
            "_anotherValidVal",

    })
    void validate_validProperty(String key) {
        var query = QuerySpec.Builder.newInstance().filter(List.of(new Criterion(key, "=", "someval"))).build();
        assertThat(validator.validate(query).succeeded()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {

            "/someValue", //leading slash
            "42ValidValues" //leading number
    })
    void validate_invalidProperty(String key) {
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion(key, "=", "something")))
                .build();

        assertThat(validator.validate(query).failed()).isTrue();
    }

}