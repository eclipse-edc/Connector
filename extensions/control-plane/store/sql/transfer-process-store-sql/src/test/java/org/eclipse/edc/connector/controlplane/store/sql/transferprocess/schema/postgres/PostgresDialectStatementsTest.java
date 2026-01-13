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
import org.eclipse.edc.sql.lease.BaseSqlLeaseStatements;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class PostgresDialectStatementsTest {

    private final LeaseStatements leaseStatements = new BaseSqlLeaseStatements();
    private final PostgresDialectStatements statements = new PostgresDialectStatements(leaseStatements, Clock.systemUTC());

    @Test
    void createQuery() {
        var q = query(criterion("id", "=", "foobar"));

        assertThat(statements.createQuery(q).getQueryAsString()).doesNotContain("json_array_elements");
    }

    @Test
    void getFormatAsJsonOperator() {
        assertThat(statements.getFormatAsJsonOperator()).isEqualTo("::json");
    }

    private QuerySpec query(Criterion criterion) {
        return QuerySpec.Builder.newInstance()
                .filter(criterion)
                .build();
    }

}
