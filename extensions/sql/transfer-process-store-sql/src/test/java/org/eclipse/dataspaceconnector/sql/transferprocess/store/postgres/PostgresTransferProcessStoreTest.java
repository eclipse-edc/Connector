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

package org.eclipse.dataspaceconnector.sql.transferprocess.store.postgres;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.sql.lease.LeaseUtil;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
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
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataAddressBuilder;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataRequest;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataRequestBuilder;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createTransferProcess;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createTransferProcessBuilder;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
class PostgresTransferProcessStoreTest {
    private static final String DATASOURCE_NAME = "transferprocess";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    private SqlTransferProcessStore store;
    private PostgresDialectStatements sqlStatements;
    private TransactionContext transactionContext;
    private LeaseUtil leaseUtil;
    private Connection connection;
    private DataSourceRegistry dataSourceRegistry;

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

        sqlStatements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();
        manager.registerTypes(TestResourceDef.class, TestProvisionedResource.class);

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
    void find() {
        var t = createTransferProcess("id1");
        store.create(t);

        var res = store.find("id1");

        assertThat(res).usingRecursiveComparison().isEqualTo(t);
    }

    @Test
    void delete() {
        var t1 = createTransferProcess("id1");
        store.create(t1);

        store.delete("id1");
        assertThat(store.findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void find_queryByDataAddressProperty() {
        var da = createDataAddressBuilder("test-type")
                .property("key", "value")
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .contentDataAddress(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataAddress.properties.key", "=", "value")))
                .build();

        var result = store.findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByDataAddress_propNotExist() {
        var da = createDataAddressBuilder("test-type")
                .property("key", "value")
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .contentDataAddress(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataAddress.properties.notexist", "=", "value")))
                .build();

        assertThat(store.findAll(query)).isEmpty();

    }

    @Test
    void find_queryByDataAddress_valueNotExist() {
        var da = createDataAddressBuilder("test-type")
                .property("key", "value")
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .contentDataAddress(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataAddress.properties.key", "=", "notexist")))
                .build();

        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void find_queryByDataRequestProperty_processId() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.processId", "=", "testprocess1")))
                .build();

        var result = store.findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByDataRequestProperty_id() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.id", "=", da.getId())))
                .build();

        var result = store.findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByDataRequestProperty_transferType() {
        var da = createDataRequestBuilder().transferType(TransferType.Builder.transferType().contentType("test/contenttype").build())
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.transferType.contentType", "like", "%/contenttype")))
                .build();

        var result = store.findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
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
    void find_queryByDataRequest_valueNotExist() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.id", "=", "notexist")))
                .build();

        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void find_queryByResourceManifestProperty() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
        var tp = createTransferProcessBuilder("testprocess1")
                .resourceManifest(rm)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.definitions.id", "=", "rd-id")))
                .build();

        var result = store.findAll(query);
        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByResourceManifest_propNotExist() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
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
    void find_queryByResourceManifest_valueNotExist() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
        var tp = createTransferProcessBuilder("testprocess1")
                .resourceManifest(rm)
                .build();
        store.create(tp);
        store.create(createTransferProcess("testprocess2"));

        // throws exception when an explicit mapping exists
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.definitions.id", "=", "someval")))
                .build();
        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void find_queryByProvisionedResourceSetProperty() {
        var resource = TestProvisionedResource.Builder.newInstance()
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

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.resources.transferProcessId", "=", "testprocess1")))
                .build();

        var result = store.findAll(query);
        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByProvisionedResourceSet_propNotExist() {
        var resource = TestProvisionedResource.Builder.newInstance()
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
    void find_queryByProvisionedResourceSet_valueNotExist() {
        var resource = TestProvisionedResource.Builder.newInstance()
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


        // returns empty when the invalid value is embedded in JSON
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.resources.id", "=", "someval")))
                .build();

        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void find_queryByDeprovisionedResourcesProperty() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();
        var dp3 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid3")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        var process2 = createTransferProcessBuilder("test-pid2")
                .deprovisionedResources(List.of(dp3))
                .build();

        store.create(process1);
        store.create(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.inProcess=true")
                .build();

        var result = store.findAll(query).collect(Collectors.toList());

        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(process1);
    }

    @Test
    void find_queryByDeprovisionedResourcesProperty_multipleCriteria() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();
        var dp3 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid3")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        var process2 = createTransferProcessBuilder("test-pid2")
                .deprovisionedResources(List.of(dp3))
                .build();

        store.create(process1);
        store.create(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(
                        new Criterion("deprovisionedResources.inProcess", "=", "false"),
                        new Criterion("id", "=", "test-pid1")
                ))
                .build();

        var result = store.findAll(query).collect(Collectors.toList());

        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(process1);
    }

    @Test
    void find_queryByDeprovisionedResourcesProperty_multipleResults() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();
        var dp3 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid3")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        var process2 = createTransferProcessBuilder("test-pid2")
                .deprovisionedResources(List.of(dp3))
                .build();

        store.create(process1);
        store.create(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.inProcess=false")
                .build();

        var result = store.findAll(query).collect(Collectors.toList());

        assertThat(result).hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(process1, process2);
    }

    @Test
    void find_queryByDeprovisionedResources_propNotExist() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        store.create(process1);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.foobar=barbaz")
                .build();

        var result = store.findAll(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByDeprovisionedResources_valueNotExist() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .errorMessage("not enough resources")
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .errorMessage("undefined error")
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        store.create(process1);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.errorMessage=notexist")
                .build();

        var result = store.findAll(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
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

    protected Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonTypeName("dataspaceconnector:testresourcedef")
    @JsonDeserialize(builder = TestResourceDef.Builder.class)
    static class TestResourceDef extends ResourceDefinition {

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ResourceDefinition.Builder<TestResourceDef, Builder> {
            private Builder() {
                super(new TestResourceDef());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }

    @JsonDeserialize(builder = TestProvisionedResource.Builder.class)
    @JsonTypeName("dataspaceconnector:testprovisionedresource")
    static class TestProvisionedResource extends ProvisionedResource {

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProvisionedResource.Builder<TestProvisionedResource, Builder> {
            private Builder() {
                super(new TestProvisionedResource());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }
}
