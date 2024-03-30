/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.policydefinition.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PostgresDialectStatementsTest {

    private final PostgresDialectStatements statements = new PostgresDialectStatements();

    @Test
    void getFormatAsJsonOperator() {
        assertThat(statements.getFormatAsJsonOperator()).isEqualTo("::json");
    }

    @ParameterizedTest
    @ArgumentsSource(JsonArrayCriteria.class)
    void createQuery_jsonArrayProperty(Criterion criterion) {
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion).build();

        var query = statements.createQuery(querySpec);

        assertThat(query.getQueryAsString()).contains("->>", "->", "json_array_elements");
    }

    @Test
    void createQuery_normalProperty() {
        var criterion = criterion("policy.assigner", "=", "foobar");
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion).build();

        var query = statements.createQuery(querySpec);

        assertThat(query.getQueryAsString()).doesNotContain("->>", "->", "json_array_elements");
    }

    private static class JsonArrayCriteria implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("policy.permissions.duties.target", "=", "true")),
                    arguments(criterion("policy.prohibitions.action.type", "=", "true")),
                    arguments(criterion("policy.obligations.assignee", "=", "true")),
                    arguments(criterion("policy.extensibleProperties.something", "=", "true"))
            );
        }
    }

}
