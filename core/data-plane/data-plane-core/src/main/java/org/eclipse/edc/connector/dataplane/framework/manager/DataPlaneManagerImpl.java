/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.framework.manager;

import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.framework.DataPlaneFrameworkExtension.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.connector.dataplane.framework.DataPlaneFrameworkExtension.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

/**
 * Default data manager implementation.
 */
public class DataPlaneManagerImpl implements DataPlaneManager {
    private int batchSize = DEFAULT_BATCH_SIZE;
    private WaitStrategy waitStrategy = () -> DEFAULT_ITERATION_WAIT;
    private PipelineService pipelineService;
    private ExecutorInstrumentation executorInstrumentation;
    private Monitor monitor;
    private Telemetry telemetry;
    private DataPlaneStore store;
    private TransferServiceRegistry transferServiceRegistry;
    private TransferProcessApiClient transferProcessClient;
    private StateMachineManager stateMachineManager;
    private EntityRetryProcessConfiguration entityRetryProcessConfiguration = defaultEntityRetryProcessConfiguration();
    private EntityRetryProcessFactory entityRetryProcessFactory;
    private Clock clock = Clock.systemUTC();

    private DataPlaneManagerImpl() {

    }

    public void start() {
        entityRetryProcessFactory = new EntityRetryProcessFactory(monitor, clock, entityRetryProcessConfiguration);
        stateMachineManager = StateMachineManager.Builder.newInstance("data-plane", monitor, executorInstrumentation, waitStrategy)
                .processor(processDataFlowInState(RECEIVED, this::processReceived))
                .processor(processDataFlowInState(COMPLETED, this::processCompleted))
                .processor(processDataFlowInState(FAILED, this::processFailed))
                .build();

        stateMachineManager.start();
    }

    public void stop() {
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest dataRequest) {
        var transferService = transferServiceRegistry.resolveTransferService(dataRequest);
        return transferService != null ?
                transferService.validate(dataRequest) :
                Result.failure(format("Cannot find a transfer Service that can handle %s source and %s destination",
                        dataRequest.getSourceDataAddress().getType(), dataRequest.getDestinationDataAddress().getType()));
    }

    @Override
    public void initiate(DataFlowRequest dataRequest) {
        var dataFlow = DataFlow.Builder.newInstance()
                .id(dataRequest.getProcessId())
                .source(dataRequest.getSourceDataAddress())
                .destination(dataRequest.getDestinationDataAddress())
                .callbackAddress(dataRequest.getCallbackAddress())
                .traceContext(telemetry.getCurrentTraceContext())
                .trackable(dataRequest.isTrackable())
                .properties(dataRequest.getProperties())
                .state(RECEIVED.code())
                .build();

        update(dataFlow);
    }

    @Override
    public CompletableFuture<StreamResult<Void>> transfer(DataSink sink, DataFlowRequest request) {
        return pipelineService.transfer(sink, request);
    }

    @Override
    public DataFlowStates transferState(String processId) {
        return Optional.ofNullable(store.findById(processId)).map(StatefulEntity::getState)
                .map(DataFlowStates::from).orElse(null);
    }

    private boolean processReceived(DataFlow dataFlow) {
        var request = dataFlow.toRequest();
        var transferService = transferServiceRegistry.resolveTransferService(request);

        if (transferService == null) {
            dataFlow.transitToFailed("No transferService available for DataFlow " + dataFlow.getId());
            update(dataFlow);
            return true;
        }

        return entityRetryProcessFactory.doAsyncProcess(dataFlow, () -> transferService.transfer(request))
                .entityRetrieve(id -> store.findById(id))
                .onSuccess((f, r) -> {
                    if (r.succeeded()) {
                        f.transitToCompleted();
                    } else {
                        f.transitToFailed(r.getFailureDetail());
                    }
                    update(f);
                })
                .onFailure((f, t) -> {
                    f.transitToReceived();
                    update(f);
                })
                .onRetryExhausted((f, t) -> {
                    f.transitToFailed(t.getMessage());
                    update(f);
                })
                .execute("start data flow");
    }

    private boolean processCompleted(DataFlow dataFlow) {
        var response = transferProcessClient.completed(dataFlow.toRequest());
        if (response.succeeded()) {
            dataFlow.transitToNotified();
            update(dataFlow);
        } else {
            dataFlow.transitToCompleted();
            update(dataFlow);
        }
        return true;
    }

    private boolean processFailed(DataFlow dataFlow) {
        var response = transferProcessClient.failed(dataFlow.toRequest(), dataFlow.getErrorDetail());
        if (response.succeeded()) {
            dataFlow.transitToNotified();
            update(dataFlow);
        } else {
            dataFlow.transitToFailed(dataFlow.getErrorDetail());
            update(dataFlow);
        }
        return true;
    }

    private Processor processDataFlowInState(DataFlowStates state, Function<DataFlow, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()) };
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    private void breakLease(DataFlow dataFlow) {
        store.save(dataFlow);
    }

    private void update(DataFlow entity) {
        store.save(entity);
        monitor.debug(format("DataFlow %s is now in state %s", entity.getId(), entity.stateAsString()));
    }

    @NotNull
    private EntityRetryProcessConfiguration defaultEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(7, () -> new ExponentialWaitStrategy(1000L));
    }

    public static class Builder {
        private final DataPlaneManagerImpl manager;

        private Builder() {
            manager = new DataPlaneManagerImpl();
            manager.telemetry = new Telemetry(); // default noop implementation
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder batchSize(int size) {
            manager.batchSize = size;
            return this;
        }

        public Builder waitStrategy(WaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return this;
        }

        public Builder pipelineService(PipelineService pipelineService) {
            manager.pipelineService = pipelineService;
            return this;
        }

        public Builder executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            manager.executorInstrumentation = executorInstrumentation;
            return this;
        }

        public Builder clock(Clock clock) {
            manager.clock = clock;
            return this;
        }

        public Builder entityRetryProcessConfiguration(EntityRetryProcessConfiguration entityRetryProcessConfiguration) {
            manager.entityRetryProcessConfiguration = entityRetryProcessConfiguration;
            return this;
        }

        public Builder transferServiceRegistry(TransferServiceRegistry transferServiceRegistry) {
            manager.transferServiceRegistry = transferServiceRegistry;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder telemetry(Telemetry telemetry) {
            manager.telemetry = telemetry;
            return this;
        }

        public Builder store(DataPlaneStore store) {
            manager.store = store;
            return this;
        }

        public Builder transferProcessClient(TransferProcessApiClient transferProcessClient) {
            manager.transferProcessClient = transferProcessClient;
            return this;
        }

        public DataPlaneManagerImpl build() {
            Objects.requireNonNull(manager.transferProcessClient);
            return manager;
        }
    }

}
