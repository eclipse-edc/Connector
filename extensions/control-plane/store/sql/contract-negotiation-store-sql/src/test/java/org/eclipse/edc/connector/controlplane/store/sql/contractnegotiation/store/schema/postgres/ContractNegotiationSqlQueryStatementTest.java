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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres;

import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.lease.BaseSqlLeaseStatements;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractNegotiationSqlQueryStatementTest {

    private static final String SELECT_STATEMENT = "SELECT * FROM test-table";
    private final ContractNegotiationStatements postresStatements = new PostgresDialectStatements(new BaseSqlLeaseStatements(), Clock.systemUTC());

    @Test
    void singleExpression_equalsOperator() {
        var criterion = new Criterion("counterPartyId", "=", "testid1");
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new ContractNegotiationMapping(postresStatements), new PostgresqlOperatorTranslator());


        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE counterparty_id = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsOnly("testid1", 50, 0);
    }

    @Test
    void singleExpression_inOperator() {
        var criterion = new Criterion("counterPartyId", "in", List.of("id1", "id2", "id3"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new ContractNegotiationMapping(postresStatements), new PostgresqlOperatorTranslator());


        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE counterparty_id IN (?,?,?) LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactlyInAnyOrder("id1", "id2", "id3", 50, 0);
    }

    @Test
    void multipleExpressions() {
        var criterion1 = new Criterion("counterPartyId", "in", List.of("id1", "id2", "id3"));
        var criterion2 = new Criterion("stateCount", "=", "4");
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion1, criterion2), new ContractNegotiationMapping(postresStatements), new PostgresqlOperatorTranslator());

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE counterparty_id IN (?,?,?) AND state_count = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactlyInAnyOrder("id1", "id2", "id3", "4", 50, 0);
    }

    @Test
    void nestedFieldAccess_inOperator() {
        var criterion = new Criterion("contractAgreement.providerId", "in", List.of("id1", "id2", "id3"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new ContractNegotiationMapping(postresStatements), new PostgresqlOperatorTranslator());

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE provider_agent_id IN (?,?,?) LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsExactlyInAnyOrder("id1", "id2", "id3", 50, 0);
    }

    @Test
    void multiNestedFieldAccess_equalsOperator() {
        var criterion = new Criterion("contractAgreement.policy.assignee", "=", "testassignee");
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new ContractNegotiationMapping(postresStatements), new PostgresqlOperatorTranslator());

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE policy ->> 'assignee' = ? LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsOnly("testassignee", 50, 0);
    }

    @Test
    void multiNestedFieldAccess_withPath_inOperator() {
        var criterion = new Criterion("contractAgreement.policy.prohibitions.constraints", "in", List.of("yomama"));
        var t = new SqlQueryStatement(SELECT_STATEMENT, query(criterion), new ContractNegotiationMapping(postresStatements), new PostgresqlOperatorTranslator());

        assertThat(t.getQueryAsString()).isEqualToIgnoringCase(SELECT_STATEMENT + " WHERE policy -> 'prohibitions' ->> 'constraints' in (?) LIMIT ? OFFSET ?;");
        assertThat(t.getParameters()).containsOnly("yomama", 50, 0);
    }

    private QuerySpec query(Criterion... criterion) {
        return QuerySpec.Builder.newInstance().filter(List.of(criterion)).build();
    }
}
