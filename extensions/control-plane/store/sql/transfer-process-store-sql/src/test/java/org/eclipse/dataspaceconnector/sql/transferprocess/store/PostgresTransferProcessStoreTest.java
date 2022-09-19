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

package org.eclipse.dataspaceconnector.sql.transferprocess.store;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.sql.lease.LeaseUtil;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.dataspaceconnector.transfer.store.TestFunctions;
import org.eclipse.dataspaceconnector.transfer.store.TransferProcessStoreTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.transfer.store.TestFunctions.createDataRequest;
import static org.eclipse.dataspaceconnector.transfer.store.TestFunctions.createTransferProcess;
import static org.eclipse.dataspaceconnector.transfer.store.TestFunctions.createTransferProcessBuilder;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
class PostgresTransferProcessStoreTest extends TransferProcessStoreTestBase {
    private static final String DATASOURCE_NAME = "transferprocess";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    private TransactionContext transactionContext;
    private Connection connection;
    private DataSourceRegistry dataSourceRegistry;
    private LeaseUtil leaseUtil;
    private SqlTransferProcessStore store;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createDatabase(POSTGRES_DATABASE);
    }

    @BeforeEach
    void setUp() throws SQLException, IOException {
        transactionContext = new NoopTransactionContext();
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

        PostgresDialectStatements sqlStatements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();
        manager.registerTypes(TestFunctions.TestResourceDef.class, TestFunctions.TestProvisionedResource.class);

        leaseUtil = new LeaseUtil(transactionContext, this::getConnection, sqlStatements, Clock.systemUTC());

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlTransferProcessStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, manager.getMapper(), sqlStatements, "test-connector", Clock.systemUTC());
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
    void find_queryByDataRequest_propNotExist() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.notexist", "=", "somevalue")))
                .build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed");
    }


    @Test
    void find_queryByResourceManifest_propNotExist() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestFunctions.TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
        var tp = createTransferProcessBuilder("testprocess1")
                .resourceManifest(rm)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        // throws exception when an explicit mapping exists
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.foobar", "=", "someval")))
                .build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

        // returns empty when the invalid value is embedded in JSON
        var query2 = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.definitions.notexist", "=", "someval")))
                .build();

        assertThat(store.findAll(query2)).isEmpty();
    }


    @Test
    void find_queryByProvisionedResourceSet_propNotExist() {
        var resource = TestFunctions.TestProvisionedResource.Builder.newInstance()
                .resourceDefinitionId("rd-id")
                .transferProcessId("testprocess1")
                .id("pr-id")
                .build();
        var prs = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(resource))
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .provisionedResourceSet(prs)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        // throws exception when an explicit mapping exists
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.foobar.transferProcessId", "=", "testprocess1")))
                .build();
        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

        // returns empty when the invalid value is embedded in JSON
        var query2 = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.resources.foobar", "=", "someval")))
                .build();

        assertThat(store.findAll(query2)).isEmpty();
    }


    @Test
    void find_queryByLease() {
        store.create(createTransferProcess("testprocess1"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("lease.leasedBy", "=", "foobar")))
                .build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    @Test
    void create_withoutDataRequest_throwsException() {
        var t1 = TestFunctions.createTransferProcessBuilder("id1")
                .dataRequest(null)
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> getTransferProcessStore().create(t1));
    }

    @AfterEach
    void tearDown() throws Exception {

        transactionContext.execute(() -> {
            var dialect = new PostgresDialectStatements();
            executeQuery(connection, "DROP TABLE " + dialect.getTransferProcessTableName() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + dialect.getDataRequestTable() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + dialect.getLeaseTableName() + " CASCADE");
        });
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Override
    @Test
    protected void findAll_verifySorting_invalidProperty() {
        IntStream.range(0, 10).forEach(i -> getTransferProcessStore().create(createTransferProcess("test-neg-" + i)));

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThatThrownBy(() -> getTransferProcessStore().findAll(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Override
    protected boolean supportsCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportsLikeOperator() {
        return true;
    }

    @Override
    protected boolean supportsInOperator() {
        return true;
    }

    @Override
    protected boolean supportsSortOrder() {
        return true;
    }

    @Override
    protected SqlTransferProcessStore getTransferProcessStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        getLeaseUtil().leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return getLeaseUtil().isLeased(negotiationId, owner);
    }

    protected LeaseUtil getLeaseUtil() {
        return leaseUtil;
    }

    protected Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
