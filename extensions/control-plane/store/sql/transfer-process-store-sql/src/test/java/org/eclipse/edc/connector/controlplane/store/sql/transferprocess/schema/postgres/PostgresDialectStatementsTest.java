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

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class PostgresDialectStatementsTest {

    private final PostgresDialectStatements statements = new PostgresDialectStatements(Clock.systemUTC());

    @Test
    void createQuery() {
        var q = query(criterion("id", "=", "foobar"));

        assertThat(statements.createQuery(q).getQueryAsString()).doesNotContain("json_array_elements");
    }

    @Test
    void createNextNotLeaseQuery_shouldFilterByRowLeaseAndSkipLockedRows() {
        var q = query(criterion("state", "=", 100));

        var query = statements.createNextNotLeaseQuery(q).getQueryAsString();

        assertThat(query)
                .contains("(edc_transfer_process.leased_at IS NULL OR (edc_transfer_process.leased_at + edc_transfer_process.lease_duration) < ?)")
                .endsWith("FOR UPDATE SKIP LOCKED;")
                .doesNotContain("edc_lease");
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
