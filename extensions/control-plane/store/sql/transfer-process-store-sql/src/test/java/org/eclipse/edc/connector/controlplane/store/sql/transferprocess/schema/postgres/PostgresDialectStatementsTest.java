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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PostgresDialectStatementsTest {

    private final PostgresDialectStatements statements = new PostgresDialectStatements();

    @Test
    void createQuery() {
        var q = query(criterion("id", "=", "foobar"));

        assertThat(statements.createQuery(q).getQueryAsString()).doesNotContain("json_array_elements");
    }

    @ParameterizedTest
    @ArgumentsSource(JsonArrayCriteria.class)
    void createQuery_isJsonArray(Criterion criterion) {
        var query = statements.createQuery(query(criterion));

        assertThat(query.getQueryAsString()).contains("->>", "->", "json_array_elements");
    }

    @Test
    void getFormatAsJsonOperator() {
        assertThat(statements.getFormatAsJsonOperator()).isEqualTo("::json");
    }

    private static class JsonArrayCriteria implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("deprovisionedResources.inProcess", "=", "true")),
                    arguments(criterion("provisionedResourceSet.resources.id", "=", "something")),
                    arguments(criterion("resourceManifest.definitions.id", "like", "%foo"))
            );
        }
    }

    private QuerySpec query(Criterion criterion) {
        return QuerySpec.Builder.newInstance()
                .filter(criterion)
                .build();
    }
}
