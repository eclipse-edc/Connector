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
import org.eclipse.edc.connector.core.entity.AbstractStateEntityManager;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

/**
 * Default data manager implementation.
 */
public class DataPlaneManagerImpl extends AbstractStateEntityManager<DataFlow, DataPlaneStore> implements DataPlaneManager {

    private PipelineService pipelineService;
    private TransferServiceRegistry transferServiceRegistry;
    private TransferProcessApiClient transferProcessClient;

    private DataPlaneManagerImpl() {

    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processDataFlowInState(RECEIVED, this::processReceived))
                .processor(processDataFlowInState(COMPLETED, this::processCompleted))
                .processor(processDataFlowInState(FAILED, this::processFailed));
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

    public static class Builder extends AbstractStateEntityManager.Builder<DataFlow, DataPlaneStore, DataPlaneManagerImpl, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new DataPlaneManagerImpl());
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder pipelineService(PipelineService pipelineService) {
            manager.pipelineService = pipelineService;
            return this;
        }

        public Builder transferServiceRegistry(TransferServiceRegistry transferServiceRegistry) {
            manager.transferServiceRegistry = transferServiceRegistry;
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
