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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.SqlContractDefinitionStore;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres.PostgresDialectStatements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.contractdefinition.store.TestFunctions.getContractDefinitions;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
public class PostgresContractDefinitionStoreTest {
    protected static final String DATASOURCE_NAME = "contractdefinition";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    private SqlContractDefinitionStore store;
    private BaseSqlDialectStatements sqlStatements;
    private TransactionContext transactionContext;
    private Connection connection;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createDatabase(POSTGRES_DATABASE);
    }

    @BeforeEach
    void setUp() throws SQLException, IOException {
        transactionContext = new NoopTransactionContext();
        DataSourceRegistry dataSourceRegistry = mock(DataSourceRegistry.class);


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

        sqlStatements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlContractDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, sqlStatements, manager);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        try {
            transactionContext.execute(() -> {
                executeQuery(connection, schema);
                return null;
            });
        } catch (Exception exc) {
            fail(exc);
        }
    }

    @Test
    void find_queryBySelectorExpression_left() {
        var definitionsExpected = getContractDefinitions(20);
        // add a selector expression to the last 5 elements
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        definitionsExpected.get(5).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        store.save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(format("selectorExpression.criteria.left = %s", Asset.PROPERTY_ID))
                .build();

        var definitionsRetrieved = store.findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .allSatisfy(cd -> assertThat(cd.getId()).matches("id[0,5]"));
    }

    @Test
    void find_queryBySelectorExpression_right() {
        var definitionsExpected = getContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        definitionsExpected.get(5).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        store.save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria.right = foobar-asset")
                .build();

        var definitionsRetrieved = store.findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(definitionsExpected.get(5));
    }

    @Test
    void find_queryMultiple() {
        var definitionsExpected = getContractDefinitions(20);
        definitionsExpected.forEach(d -> d.getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset")));
        store.save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("selectorExpression.criteria.right", "=", "test-asset"),
                        new Criterion("contractPolicyId", "=", "contract4")))
                .build();

        var definitionsRetrieved = store.findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(definitionsExpected.get(4));
    }

    @Test
    void find_queryMultiple_noMatch() {
        var definitionsExpected = getContractDefinitions(20);
        store.save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("selectorExpression.criteria.right", "=", "test-asset"),
                        new Criterion("contractPolicyId", "=", "contract4")))
                .build();

        assertThat(store.findAll(spec).collect(Collectors.toList())).isEmpty();
    }

    @Test
    void find_queryBySelectorExpression_entireCriterion() throws JsonProcessingException {
        var definitionsExpected = getContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        var def5 = definitionsExpected.get(5);
        def5.getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        store.save(definitionsExpected);

        var json = new ObjectMapper().writeValueAsString(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria = " + json)
                .build();

        assertThat(store.findAll(spec)).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(def5);
    }


}
