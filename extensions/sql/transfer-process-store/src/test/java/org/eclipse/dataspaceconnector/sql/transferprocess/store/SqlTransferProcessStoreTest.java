/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.dataspaceconnector.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.dataspaceconnector.sql.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.transaction.local.DataSourceResource;
import org.eclipse.dataspaceconnector.transaction.local.LocalDataSourceRegistry;
import org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataRequest;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createTransferProcess;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;

class SqlTransferProcessStoreTest {
    private static final String DATASOURCE_NAME = "transferprocess";
    private static final String CONNECTOR_NAME = "test-connector";
    private DataSourceRegistry dataSourceRegistry;
    private ConnectionPool connectionPool;
    private SqlTransferProcessStore store;
    private SqlLeaseContextBuilder leaseContext;
    private TransactionContext transactionContext;

    @BeforeEach
    void setUp() throws SQLException {
        var monitor = new ConsoleMonitor();
        var txManager = new LocalTransactionContext(monitor);
        dataSourceRegistry = new LocalDataSourceRegistry(txManager);
        transactionContext = txManager;
        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        var connection = jdbcDataSource.getConnection();
        var dataSource = new ConnectionFactoryDataSource(() -> connection);
        connectionPool = new CommonsConnectionPool(dataSource, CommonsConnectionPoolConfig.Builder.newInstance().build());
        var poolDataSource = new ConnectionPoolDataSource(connectionPool);
        dataSourceRegistry.register(DATASOURCE_NAME, poolDataSource);
        txManager.registerResource(new DataSourceResource(poolDataSource));
        store = new SqlTransferProcessStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new ObjectMapper(), new PostgresStatements(), CONNECTOR_NAME);

        try (var inputStream = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            var schema = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            transactionContext.execute(() -> executeQuery(connection, schema));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        leaseContext = SqlLeaseContextBuilder.with(transactionContext, CONNECTOR_NAME, new PostgresStatements());
    }

    @AfterEach
    void tearDown() throws Exception {
        connectionPool.close();
    }

    @Test
    void create() {
        var t = createTransferProcess("test-id");
        store.create(t);

        assertThat(store.findAll(QuerySpec.none())).containsExactly(t);
    }

    @Test
    void create_withSameIdExists_shouldReplace() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        var t2 = createTransferProcess("id1", TransferProcessStates.PROVISIONING);
        store.create(t2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(1).containsExactly(t2);
    }

    @Test
    void nextForState() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        assertThat(store.nextForState(state.code(), 5))
                .hasSize(5)
                .extracting(TransferProcess::getId)
                .isSubsetOf(all.stream().map(TransferProcess::getId).collect(Collectors.toList()))
                .allMatch(id -> isLeased(id, CONNECTOR_NAME));
    }

    @Test
    void nextForState_shouldOnlyReturnFreeItems() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> leaseEntity(tp.getId(), CONNECTOR_NAME)).collect(Collectors.toList());

        // should not contain leased TPs
        assertThat(store.nextForState(state.code(), 10))
                .hasSize(5)
                .isSubsetOf(all)
                .doesNotContainAnyElementsOf(leasedTp);

    }


    @Test
    void nextForState_noFreeItem_shouldReturnEmpty() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(store::create);

        // first time works
        assertThat(store.nextForState(state.code(), 10)).hasSize(3);
        // second time returns empty list
        assertThat(store.nextForState(state.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_noneInDesiredState() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(store::create);

        assertThat(store.nextForState(TransferProcessStates.CANCELLED.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_batchSizeLimits() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(store::create);

        // first time works
        assertThat(store.nextForState(state.code(), 3)).hasSize(3);
    }

    @Test
    void nextForState_verifyTemporalOrdering() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i), state))
                .peek(t -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                        // noop
                    }
                    t.updateStateTimestamp();
                })
                .forEach(store::create);

        assertThat(store.nextForState(state.code(), 20))
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    void nextForState_verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        Thread.sleep(100);

        var fourth = all.get(3);
        fourth.updateStateTimestamp();
        store.update(fourth);

        var next = store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 20);
        assertThat(next.indexOf(fourth)).isEqualTo(9);
    }

    @Test
    @DisplayName("Verifies that calling nextForState locks the TP for any subsequent calls")
    void nextForState_locksEntity() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        store.nextForState(TransferProcessStates.UNSAVED.code(), 100);

        assertThat(isLeased(t.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextForState_expiredLease() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        leaseEntity(t.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> store.nextForState(TransferProcessStates.UNSAVED.code(), 10), hasSize(1));
    }

    @Test
    void find() {
        var t = createTransferProcess("id1");
        store.create(t);

        TransferProcess res = store.find("id1");
        assertThat(res).usingRecursiveComparison().isEqualTo(t);
    }

    @Test
    void find_notExist() {
        assertThat(store.find("not-exist")).isNull();
    }

    @Test
    void processIdForTransferId() {
        var pid = "process-id1";
        var tid = "transfer-id1";

        var dr = createDataRequest(pid);
        var t = createTransferProcess(tid, dr);

        store.create(t);

        assertThat(store.processIdForTransferId(tid)).isEqualTo("transfer-id1");
    }

    @Test
    void processIdForTransferId_notExist() {
        assertThat(store.processIdForTransferId("not-exist")).isNull();
    }

    @Test
    void update_exists_shouldUpdate() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);
        store.create(t1);

        t1.transitionCompleted(); //modify
        store.update(t1);

        assertThat(store.findAll(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    void update_notExist_shouldCreate() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);

        t1.transitionCompleted(); //modify
        store.update(t1);

        var result = store.findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(result)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    @DisplayName("Verify that the lease on a TP is cleared by an update")
    void update_shouldBreakLease() {
        var t1 = createTransferProcess("id1");
        store.create(t1);
        // acquire lease
        leaseEntity(t1.getId(), CONNECTOR_NAME);

        t1.transitionInitial(); //modify
        store.update(t1);

        // lease should be broken
        assertThat(store.nextForState(TransferProcessStates.INITIAL.code(), 10)).containsExactly(t1);
    }

    @Test
    void update_leasedByOther_shouldThrowException() {

        var tpId = "id1";
        var t1 = createTransferProcess(tpId);
        store.create(t1);
        leaseEntity(tpId, "someone");

        t1.transitionInitial(); //modify

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> store.update(t1)).hasRootCauseInstanceOf(IllegalStateException.class);

    }

    @Test
    void delete() {
        var t1 = createTransferProcess("id1");
        store.create(t1);

        store.delete("id1");
        assertThat(count()).isEqualTo(0);
        assertThat(store.findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void delete_isLeasedBySelf_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        store.create(t1);
        leaseEntity(t1.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> store.delete("id1")).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        store.create(t1);

        leaseEntity(t1.getId(), "someone-else");

        assertThatThrownBy(() -> store.delete("id1")).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_notExist() {
        store.delete("not-exist");
        //no exception should be raised
    }

    @Test
    void findAll_noQuerySpec() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i))
                .peek(store::create)
                .collect(Collectors.toList());

        assertThat(store.findAll(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void findAll_verifyPaging() {
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(store::create);

        var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
        assertThat(store.findAll(qs)).hasSize(5)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 8);
    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(store::create);

        var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
        assertThat(store.findAll(qs))
                .hasSize(7)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 10);
    }

    @Test
    void findAll_verifyPaging_pageSizeOutsideCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(store::create);

        var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
        assertThat(store.findAll(qs)).isEmpty();

    }

    private boolean isLeased(String id, String connectorName) {
        try (var conn = dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection()) {

            var lease = leaseContext.withConnection(conn).getLease(id);
            return lease != null && lease.getLeasedBy().equals(connectorName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int count() {
        try (var conn = dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection()) {
            return executeQuery(conn, "SELECT COUNT(*) FROM edc_transfer_process");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void leaseEntity(String tpId, String leaseHolder) {
        try (var conn = dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection()) {
            leaseContext.by(leaseHolder).withConnection(conn).acquireLease(tpId);
        } catch (SQLException e) {
            fail(e);
        }
    }

    private void leaseEntity(String tpId, String leaseHolder, Duration leaseDuration) {
        try (var conn = dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection()) {
            leaseContext.by(leaseHolder).forTime(leaseDuration).withConnection(conn).acquireLease(tpId);
        } catch (SQLException e) {
            fail(e);
        }
    }

}