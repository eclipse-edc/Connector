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

package org.eclipse.edc.connector.store.sql.policydefinition.schema.postgres;

import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createQuery;

class PostgresDialectStatementsTest {

    private final PostgresDialectStatements statements = new PostgresDialectStatements();

    @Test
    void getFormatAsJsonOperator() {
        assertThat(statements.getFormatAsJsonOperator()).isEqualTo("::json");
    }

    @Test
    void createQuery_jsonArrayProperty() {
        assertThat(statements.createQuery(createQuery("policy.permissions.duties.target=foo")).getQueryAsString()).contains("->>", "->", "json_array_elements");
        assertThat(statements.createQuery(createQuery("policy.prohibitions.action.type=foo")).getQueryAsString()).contains("->>", "->", "json_array_elements");
        assertThat(statements.createQuery(createQuery("policy.obligations.assignee=foo")).getQueryAsString()).contains("->>", "->", "json_array_elements");
        assertThat(statements.createQuery(createQuery("policy.extensibleProperties.something=foo")).getQueryAsString()).contains("->>", "->", "json_array_elements");
    }

    @Test
    void createQuery_normalProperty() {
        var q = createQuery("policy.assigner=foobar");
        assertThat(statements.createQuery(q).getQueryAsString()).doesNotContain("->>", "->", "json_array_elements");
    }
}
