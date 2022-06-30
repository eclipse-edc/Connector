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

package org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresDialectStatementsTest {

    private final PostgresDialectStatements statements = new PostgresDialectStatements();

    @Test
    void createQuery() {
        var q = query("id=foobar");

        assertThat(statements.createQuery(q).getQueryAsString()).doesNotContain("json_array_elements");
    }

    @Test
    void createQuery_isJsonArray() {
        assertThat(statements.createQuery(query("deprovisionedResources.inProcess=true")).getQueryAsString()).contains("->>", "->", "json_array_elements");
        assertThat(statements.createQuery(query("provisionedResourceSet.resources.id=something")).getQueryAsString()).contains("->>", "->", "json_array_elements");
        assertThat(statements.createQuery(query("resourceManifest.definitions.id like %foo")).getQueryAsString()).contains("->>", "->", "json_array_elements");
    }

    @Test
    void getFormatAsJsonOperator() {
        assertThat(statements.getFormatAsJsonOperator()).isEqualTo("::json");
    }

    private QuerySpec query(String filter) {
        return QuerySpec.Builder.newInstance()
                .filter(filter)
                .build();
    }
}