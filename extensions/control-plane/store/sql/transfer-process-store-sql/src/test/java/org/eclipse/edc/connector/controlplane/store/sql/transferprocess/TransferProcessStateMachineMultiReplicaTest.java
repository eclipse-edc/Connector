/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess;

import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.process.TransferProcessManagerImpl;
import org.eclipse.edc.connector.controlplane.transfer.processors.TransferProcessorsImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store.TestFunctions.createTransferProcessBuilder;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the real poll-based transfer process state machine ({@link TransferProcessManagerImpl} with
 * {@link TransferProcessorsImpl}) in several replicas against the same Postgres database. Every replica has its own
 * {@link SqlTransferProcessStore} and lease holder, while the counter-party is a shared fake dispatcher that records
 * every message it receives.
 * <p>
 * The entities must be processed exactly once across the replicas: each one is leased by a single replica, which sends
 * its message once and saves its new state.
 */
@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class TransferProcessStateMachineMultiReplicaTest {

    private static final long ITERATION_WAIT_MILLIS = 50;
    private static final int BATCH_SIZE = 10;
    private static final Duration LEASE_DURATION = Duration.ofSeconds(60);

    private final PostgresDialectStatements statements = new PostgresDialectStatements(Clock.systemUTC());
    private final Clock clock = Clock.systemUTC();
    private final JacksonTypeManager typeManager = new JacksonTypeManager();
    private final Monitor monitor = new ConsoleMonitor(ConsoleMonitor.Level.WARNING, true);
    private final Map<String, List<String>> dispatchedMessageIds = new ConcurrentHashMap<>();
    private final List<TransferProcessManagerImpl> replicas = new ArrayList<>();
    private LocalTransactionContext transactionContext;
    private LocalDataSourceRegistry dataSourceRegistry;
    private PostgresqlStoreSetupExtension extension;
    private QueryExecutor queryExecutor;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        this.extension = extension;
        this.queryExecutor = queryExecutor;
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        transactionContext = new LocalTransactionContext(monitor);
        dataSourceRegistry = new LocalDataSourceRegistry(transactionContext);
        dataSourceRegistry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        extension.runQuery(TestUtils.getResourceFileContentAsString("transfer-process-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        replicas.forEach(TransferProcessManagerImpl::stop);
        extension.runQuery("DROP TABLE " + statements.getTransferProcessTableName() + " CASCADE");
    }

    @Test
    void multipleReplicas_shouldDispatchEveryEntityOnce() {
        var seedStore = createStore("seed");
        var ids = IntStream.range(0, 60).mapToObj(i -> requestingTransferProcess()).peek(seedStore::save).map(TransferProcess::getId).toList();

        IntStream.range(0, 3).forEach(i -> startReplica("replica-" + i));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(ids).allSatisfy(id -> assertThat(seedStore.findById(id).getState()).isEqualTo(REQUESTED.code())));
        assertThat(dispatchedMessageIds).containsOnlyKeys(ids);
        assertThat(dispatchedMessageIds.values()).allSatisfy(messageIds -> assertThat(messageIds).hasSize(1));
    }

    private void startReplica(String name) {
        var store = createStore(name);

        var policyArchive = mock(PolicyArchive.class);
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().assignee("consumer").assigner("provider").build());
        when(policyArchive.getAgreementIdForContract(any())).thenReturn("agreementId");
        var dataAddressStore = mock(DataAddressStore.class);
        when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("not found"));
        var webhookResolver = mock(ProtocolWebhookResolver.class);
        when(webhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback");
        var dispatcher = mock(ProtocolRemoteMessageDispatcher.class);
        when(dispatcher.dispatch(any(), any(), any())).thenAnswer(invocation -> {
            TransferRequestMessage message = invocation.getArgument(2);
            dispatchedMessageIds.computeIfAbsent(message.getConsumerPid(), id -> new CopyOnWriteArrayList<>()).add(message.getId());
            return completedFuture(StatusResult.success(ack()));
        });

        var retryConfiguration = new EntityRetryProcessConfiguration(7, () -> new ExponentialWaitStrategy(60_000L));
        var processors = new TransferProcessorsImpl(policyArchive, new EntityRetryProcessFactory(monitor, clock, retryConfiguration),
                mock(DataFlowController.class), dataAddressStore, new TransferProcessObservableImpl(), store, monitor,
                mock(DataAddressResolver.class), webhookResolver, dispatcher);

        var manager = TransferProcessManagerImpl.Builder.newInstance()
                .transferProcessors(processors)
                .waitStrategy(() -> ITERATION_WAIT_MILLIS)
                .batchSize(BATCH_SIZE)
                .monitor(monitor)
                .clock(clock)
                .store(store)
                .entityRetryProcessConfiguration(retryConfiguration)
                .build();
        replicas.add(manager);
        manager.start();
    }

    private TransferProcess requestingTransferProcess() {
        return createTransferProcessBuilder(UUID.randomUUID().toString())
                .state(REQUESTING.code())
                .counterPartyAddress("http://counter.party")
                .protocol("protocol")
                .contractId("contractId")
                .build();
    }

    private TransferProcessAck ack() {
        return TransferProcessAck.Builder.newInstance().providerPid(UUID.randomUUID().toString()).build();
    }

    private SqlTransferProcessStore createStore(String leaseHolder) {
        return new SqlTransferProcessStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext,
                typeManager::getMapper, statements, queryExecutor, leaseHolder, clock, LEASE_DURATION);
    }
}
