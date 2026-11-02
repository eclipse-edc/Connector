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

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.spi.testfixtures.store.PolicyMonitorStoreTestBase;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PostgresPolicyMonitorStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.STARTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;


@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresPolicyMonitorStoreTest extends PolicyMonitorStoreTestBase {

    private final PolicyMonitorStatements statements = new PostgresPolicyMonitorStatements();
    private final Clock clock = Clock.systemUTC();
    private PostgresqlStoreSetupExtension extension;
    private QueryExecutor queryExecutor;
    private SqlPolicyMonitorStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        this.extension = extension;
        this.queryExecutor = queryExecutor;
        store = createStore(CONNECTOR_NAME);
        extension.runQuery(TestUtils.getResourceFileContentAsString("policy-monitor-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getPolicyMonitorTable() + " CASCADE");
    }

    @Override
    protected PolicyMonitorStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String entityId, String owner, Duration duration) {
        var sql = "UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?".formatted(statements.getPolicyMonitorTable(),
                statements.getLeasedByColumn(), statements.getLeasedAtColumn(), statements.getLeaseDurationColumn(), statements.getIdColumn());
        extension.getTransactionContext().execute(() -> {
            try (var connection = extension.getConnection()) {
                queryExecutor.execute(connection, sql, owner, clock.millis(), duration.toMillis(), entityId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected boolean isLeasedBy(String entityId, String owner) {
        return owner.equals(leasedBy(entityId));
    }

    private @Nullable String leasedBy(String entityId) {
        var sql = "SELECT %s FROM %s WHERE %s = ?".formatted(statements.getLeasedByColumn(), statements.getPolicyMonitorTable(), statements.getIdColumn());
        return extension.getTransactionContext().execute(() -> {
            try (var connection = extension.getConnection()) {
                return queryExecutor.single(connection, false, rs -> Optional.ofNullable(rs.getString(1)), sql, entityId).orElse(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private SqlPolicyMonitorStore createStore(String leaseHolder) {
        return new SqlPolicyMonitorStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(),
                statements, new JacksonTypeManager()::getMapper, queryExecutor, leaseHolder, clock);
    }

    private PolicyMonitorEntry createEntry() {
        return PolicyMonitorEntry.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .state(STARTED.code())
                .build();
    }

    @Nested
    class RowLease {

        /**
         * Concurrent runtimes are covered by {@link SqlPolicyMonitorStoreConcurrencyTest}, which runs them in real
         * transactions: here every statement is committed on its own, so the outcome would depend on thread scheduling.
         */
        @Test
        void nextNotLeased_shouldNotReturnEntriesLeasedByAnotherRuntime() {
            var all = range(0, 50).mapToObj(i -> createEntry()).peek(store::save).map(PolicyMonitorEntry::getId).toList();
            var otherStore = createStore("other-connector");

            var firstIds = store.nextNotLeased(30, hasState(STARTED.code())).stream().map(PolicyMonitorEntry::getId).toList();
            var secondIds = otherStore.nextNotLeased(30, hasState(STARTED.code())).stream().map(PolicyMonitorEntry::getId).toList();

            assertThat(firstIds).hasSize(30);
            assertThat(secondIds).hasSize(20).noneMatch(firstIds::contains);
            assertThat(all).containsExactlyInAnyOrderElementsOf(Stream.concat(firstIds.stream(), secondIds.stream()).toList());
            assertThat(firstIds).allMatch(id -> isLeasedBy(id, CONNECTOR_NAME));
            assertThat(secondIds).allMatch(id -> isLeasedBy(id, "other-connector"));
        }

        @Test
        void save_shouldBreakLease() {
            var entry = createEntry();
            store.save(entry);
            var leased = store.nextNotLeased(1, hasState(STARTED.code()));
            assertThat(leased).hasSize(1);

            store.save(leased.get(0));

            assertThat(leasedBy(entry.getId())).isNull();
        }

        @Test
        void breakLease_shouldSucceed_whenLeasedByCurrentRuntime() {
            var entry = createEntry();
            store.save(entry);
            leaseEntity(entry.getId(), CONNECTOR_NAME);

            var result = store.breakLease(entry);

            assertThat(result).isSucceeded();
            assertThat(leasedBy(entry.getId())).isNull();
        }

        @Test
        void breakLease_shouldFail_whenLeasedByOtherRuntime() {
            var entry = createEntry();
            store.save(entry);
            leaseEntity(entry.getId(), "other-connector");

            var result = store.breakLease(entry);

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_LEASED);
            assertThat(isLeasedBy(entry.getId(), "other-connector")).isTrue();
        }
    }
}
