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
import org.eclipse.dataspaceconnector.common.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
import org.eclipse.dataspaceconnector.sql.lease.LeaseUtil;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataRequest;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createTransferProcess;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ComponentTest
class SqlTransferProcessStoreTest {
    private static final String DATASOURCE_NAME = "transferprocess";
    private static final String CONNECTOR_NAME = "test-connector";
    private SqlTransferProcessStore store;
    private LeaseUtil leaseUtil;
    private Connection connection;
    private DataSourceRegistry dataSourceRegistry;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        var transactionContext = new NoopTransactionContext();
        dataSourceRegistry = mock(DataSourceRegistry.class);

        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        // do not actually close
        connection = spy(jdbcDataSource.getConnection());
        doNothing().when(connection).close();

        var datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);
        var statements = new PostgresStatements();
        store = new SqlTransferProcessStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new ObjectMapper(), statements, CONNECTOR_NAME);

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        transactionContext.execute(() -> SqlQueryExecutor.executeQuery(connection, schema));

        leaseUtil = new LeaseUtil(transactionContext, this::getConnection, statements);

    }

    @AfterEach
    void tearDown() throws Exception {
        doCallRealMethod().when(connection).close();
        connection.close();
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
                .allMatch(id -> leaseUtil.isLeased(id, CONNECTOR_NAME));
    }

    @Test
    void nextForState_shouldOnlyReturnFreeItems() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> leaseUtil.leaseEntity(tp.getId(), CONNECTOR_NAME)).collect(Collectors.toList());

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

        assertThat(leaseUtil.isLeased(t.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextForState_expiredLease() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        leaseUtil.leaseEntity(t.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> store.nextForState(TransferProcessStates.UNSAVED.code(), 10), hasSize(1));
    }

    @Test
    void find() {
        var t = createTransferProcess("id1");
        store.create(t);

        var res = store.find("id1");

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
        leaseUtil.leaseEntity(t1.getId(), CONNECTOR_NAME);

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
        leaseUtil.leaseEntity(tpId, "someone");

        t1.transitionInitial(); //modify

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> store.update(t1)).isInstanceOf(IllegalStateException.class);

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
        leaseUtil.leaseEntity(t1.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> store.delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        store.create(t1);

        leaseUtil.leaseEntity(t1.getId(), "someone-else");

        assertThatThrownBy(() -> store.delete("id1")).isInstanceOf(IllegalStateException.class);
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

    private Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection();
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
}