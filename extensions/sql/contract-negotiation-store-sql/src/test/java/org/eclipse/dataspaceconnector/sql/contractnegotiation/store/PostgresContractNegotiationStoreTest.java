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

package org.eclipse.dataspaceconnector.sql.contractnegotiation.store;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.dataspaceconnector.sql.lease.LeaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createContract;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createContractBuilder;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createNegotiation;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * This test aims to verify those parts of the contract negotiation store, that are specific to Postgres, e.g. JSON
 * query operators.
 */
@PostgresqlDbIntegrationTest
class PostgresContractNegotiationStoreTest {
    protected static final String DATASOURCE_NAME = "contractnegotiation";
    protected static final String CONNECTOR_NAME = "test-connector";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    private static final String JDBC_URL_PREFIX = "jdbc:postgresql://localhost:5432/";
    protected DataSourceRegistry dataSourceRegistry;
    protected Connection connection;
    protected SqlContractNegotiationStore store;
    protected LeaseUtil leaseUtil;
    private TransactionContext txManager;

    @BeforeAll
    static void prepare() {
        // todo: reuse org.eclipse.dataspaceconnector.test.e2e.postgresql.PostgresqlLocalInstance??
        try (var connection = DriverManager.getConnection(JDBC_URL_PREFIX + POSTGRES_USER, POSTGRES_USER, POSTGRES_PASSWORD)) {
            connection.createStatement().execute(format("CREATE DATABASE %s;", POSTGRES_DATABASE));
        } catch (SQLException e) {
            // database could already exist
        }
    }


    @BeforeEach
    void setUp() throws SQLException, IOException {
        txManager = new NoopTransactionContext();
        dataSourceRegistry = mock(DataSourceRegistry.class);


        var ds = new PGSimpleDataSource();
        ds.setServerNames(new String[]{ "localhost" });
        ds.setPortNumbers(new int[]{ 5432 });
        ds.setUser(POSTGRES_USER);
        ds.setPassword(POSTGRES_PASSWORD);
        ds.setDatabaseName(POSTGRES_DATABASE);

        // do not actually close
        connection = spy(ds.getConnection());
        doNothing().when(connection).close();

        var datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);

        var statements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlContractNegotiationStore(dataSourceRegistry, DATASOURCE_NAME, txManager, manager, statements, CONNECTOR_NAME, Clock.systemUTC());

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        try {
            txManager.execute(() -> {
                executeQuery(connection, schema);
                return null;
            });
        } catch (Exception exc) {
            fail(exc);
        }
        leaseUtil = new LeaseUtil(txManager, this::getConnection, statements, Clock.systemUTC());
    }

    @AfterEach
    void tearDown() throws Exception {

        txManager.execute(() -> {
            var dialect = new PostgresDialectStatements();
            executeQuery(connection, "DROP TABLE " + dialect.getContractNegotiationTable() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + dialect.getContractAgreementTable() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + dialect.getLeaseTableName() + " CASCADE");
            executeQuery(connection, "DROP VIEW IF EXISTS " + dialect.getViewName() + " CASCADE");
        });
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Test
    void query_byAgreementId() {

        var agreement1 = createContract("agr1");
        var agreement2 = createContract("agr2");
        var negotiation1 = createNegotiation("neg1", agreement1);
        var negotiation2 = createNegotiation("neg2", agreement2);
        store.save(negotiation1);
        store.save(negotiation2);

        var expression = "contractAgreement.id = agr1";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation1);
    }

    @Test
    void query_byPolicyAssignee() {

        var policy = Policy.Builder.newInstance()
                .assignee("test-assignee")
                .assigner("test-assigner")
                .permission(Permission.Builder.newInstance()
                        .target("")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("foo"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("bar"))
                                .build())
                        .build())
                .build();

        var agreement1 = createContractBuilder("agr1").policy(policy).build();
        var agreement2 = createContractBuilder("agr2").policy(policy).build();
        var negotiation1 = createNegotiation("neg1", agreement1);
        var negotiation2 = createNegotiation("neg2", agreement2);
        store.save(negotiation1);
        store.save(negotiation2);

        var expression = "contractAgreement.policy.assignee = test-assignee";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(negotiation1, negotiation2);
    }

    @Test
    void query_invalidKey_shouldThrowException() {
        var agreement1 = createContract("agr1");
        var negotiation1 = createNegotiation("neg1", agreement1);
        store.save(negotiation1);

        var expression = "contractAgreement.notexist = agr1";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        assertThatThrownBy(() -> store.queryNegotiations(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Test
    void query_invalidKeyInJson() {
        var agreement1 = createContract("agr1");
        var negotiation1 = createNegotiation("neg1", agreement1);
        store.save(negotiation1);

        var expression = "contractAgreement.policy.permissions.notexist = foobar";
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        assertThat(store.queryNegotiations(query)).isEmpty();
    }

    @Test
    void queryAgreements_withQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter("assetId = asset-2").build();
        var all = store.queryAgreements(query);

        assertThat(all).hasSize(1);
    }

    @Test
    void queryAgreements_withQuerySpec_invalidOperand() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter("notexistprop = asset-2").build();
        assertThatThrownBy(() -> store.queryAgreements(query));
    }

    @Test
    void queryAgreements_withQuerySpec_noFilter() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().offset(2).limit(2).build();
        assertThat(store.queryAgreements(query)).hasSize(2);
    }

    @Test
    void queryAgreements_withQuerySpec_invalidValue() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.createContractId(UUID.randomUUID().toString()))
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter("assetId = notexist").build();
        assertThat(store.queryAgreements(query)).isEmpty();
    }

    protected Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
