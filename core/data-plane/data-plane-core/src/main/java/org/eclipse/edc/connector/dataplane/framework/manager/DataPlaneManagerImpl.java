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

import org.eclipse.edc.connector.controlplane.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.framework.DataPlaneFrameworkExtension.FlowLeaseConfiguration;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;

/**
 * Default data manager implementation.
 */
public class DataPlaneManagerImpl extends AbstractStateEntityManager<DataFlow, DataPlaneStore> implements DataPlaneManager {

    private DataPlaneAuthorizationService authorizationService;
    private TransferServiceRegistry transferServiceRegistry;
    private TransferProcessApiClient transferProcessClient;
    private String runtimeId;
    private FlowLeaseConfiguration flowLeaseConfiguration = new FlowLeaseConfiguration();

    private DataPlaneManagerImpl() {

    }

    @Override
    public Result<Boolean> validate(DataFlowStartMessage dataRequest) {
        // TODO for now no validation for pull scenario, since the transfer service registry
        //  is not applicable here. Probably validation only on the source part required.
        if (PULL.equals(dataRequest.getFlowType())) {
            return success(true);
        } else {
            var transferService = transferServiceRegistry.resolveTransferService(dataRequest);
            return transferService != null ?
                    transferService.validate(dataRequest) :
                    Result.failure(format("Cannot find a transfer Service that can handle %s source and %s destination",
                            dataRequest.getSourceDataAddress().getType(), dataRequest.getDestinationDataAddress().getType()));
        }
    }

    @Override
    public Result<DataFlowResponseMessage> start(DataFlowStartMessage startMessage) {
        var dataFlowBuilder = DataFlow.Builder.newInstance()
                .id(startMessage.getProcessId())
                .source(startMessage.getSourceDataAddress())
                .destination(startMessage.getDestinationDataAddress())
                .callbackAddress(startMessage.getCallbackAddress())
                .traceContext(telemetry.getCurrentTraceContext())
                .properties(startMessage.getProperties())
                .transferType(startMessage.getTransferType())
                .runtimeId(runtimeId);

        var response = switch (startMessage.getFlowType()) {
            case PULL -> handlePull(startMessage, dataFlowBuilder);
            case PUSH -> handlePush(startMessage, dataFlowBuilder);
        };

        return response.onSuccess(m -> update(dataFlowBuilder.build()));
    }

    @Override
    public DataFlowStates getTransferState(String processId) {
        return Optional.ofNullable(store.findById(processId)).map(StatefulEntity::getState)
                .map(DataFlowStates::from).orElse(null);
    }

    @Override
    public StatusResult<Void> suspend(String dataFlowId) {
        return stop(dataFlowId)
                .map(dataFlow -> {
                    dataFlow.transitToSuspended();
                    store.save(dataFlow);
                    return null;
                });
    }

    @Override
    public StatusResult<Void> terminate(String dataFlowId, @Nullable String reason) {
        return stop(dataFlowId, reason)
                .map(dataFlow -> {
                    dataFlow.transitToTerminated(reason);
                    store.save(dataFlow);
                    return null;
                });
    }

    @Override
    public StatusResult<Void> restartFlows() {
        var now = clock.millis();
        List<DataFlow> toBeRestarted;
        do {
            toBeRestarted = store.nextNotLeased(batchSize,
                    hasState(STARTED.code()),
                    new Criterion("stateTimestamp", "<", now),
                    new Criterion("transferType.flowType", "=", PUSH.toString())
            );

            toBeRestarted.forEach(this::restartFlow);
        } while (!toBeRestarted.isEmpty());

        return StatusResult.success();
    }
    
    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        Supplier<Criterion> ownedByThisRuntime = () -> new Criterion("runtimeId", "=", runtimeId);
        Supplier<Criterion> ownedByAnotherRuntime = () -> new Criterion("runtimeId", "!=", runtimeId);
        Supplier<Criterion> flowLeaseNeedsToBeUpdated = () -> new Criterion("updatedAt", "<", clock.millis() - flowLeaseConfiguration.time());
        Supplier<Criterion> danglingTransfer = () -> new Criterion("updatedAt", "<", clock.millis() - flowLeaseConfiguration.abandonTime());

        return builder
                .processor(processDataFlowInState(STARTED, this::updateFlowLease, ownedByThisRuntime, flowLeaseNeedsToBeUpdated))
                .processor(processDataFlowInState(STARTED, this::restartFlow, ownedByAnotherRuntime, danglingTransfer))
                .processor(processDataFlowInState(RECEIVED, this::processReceived))
                .processor(processDataFlowInState(COMPLETED, this::processCompleted))
                .processor(processDataFlowInState(FAILED, this::processFailed));
    }

    private boolean updateFlowLease(DataFlow dataFlow) {
        dataFlow.transitToReceived();
        dataFlow.transitionToStarted(runtimeId);
        store.save(dataFlow);
        return true;
    }

    private boolean restartFlow(DataFlow dataFlow) {
        monitor.debug("Restarting interrupted flow %s, it was owned by runtime %s".formatted(dataFlow.getId(), dataFlow.getRuntimeId()));
        dataFlow.transitToReceived();
        processReceived(dataFlow);
        return true;
    }

    private StatusResult<DataFlow> stop(String dataFlowId) {
        return stop(dataFlowId, null);
    }

    private StatusResult<DataFlow> stop(String dataFlowId, String reason) {
        var result = store.findByIdAndLease(dataFlowId);
        if (result.failed()) {
            return StatusResult.from(result).mapFailure();
        }

        var dataFlow = result.getContent();

        if (FlowType.PUSH.equals(dataFlow.getTransferType().flowType())) {
            var transferService = transferServiceRegistry.resolveTransferService(dataFlow.toRequest());

            if (transferService == null) {
                return StatusResult.failure(FATAL_ERROR, "TransferService cannot be resolved for DataFlow %s".formatted(dataFlowId));
            }

            var terminateResult = transferService.terminate(dataFlow);
            if (terminateResult.failed()) {
                if (terminateResult.reason().equals(StreamFailure.Reason.NOT_FOUND)) {
                    monitor.warning("No source was found for DataFlow '%s'. This may indicate an inconsistent state.".formatted(dataFlowId));
                } else {
                    return StatusResult.failure(FATAL_ERROR, "DataFlow %s cannot be terminated: %s".formatted(dataFlowId, terminateResult.getFailureDetail()));
                }
            }
        } else {
            var revokeResult = authorizationService.revokeEndpointDataReference(dataFlowId, reason);
            if (revokeResult.failed()) {
                return StatusResult.failure(FATAL_ERROR, "DataFlow %s cannot be terminated: %s".formatted(dataFlowId, revokeResult.getFailureDetail()));
            }
        }

        return StatusResult.success(dataFlow);
    }

    private Result<DataFlowResponseMessage> handlePull(DataFlowStartMessage startMessage, DataFlow.Builder dataFlowBuilder) {
        return authorizationService.createEndpointDataReference(startMessage)
                .onSuccess(dataAddress -> dataFlowBuilder.state(STARTED.code()))
                .onFailure(f -> monitor.warning("Error obtaining EDR DataAddress: %s".formatted(f.getFailureDetail())))
                .map(dataAddress -> DataFlowResponseMessage.Builder.newInstance()
                        .dataAddress(dataAddress)
                        .build());
    }

    private Result<DataFlowResponseMessage> handlePush(DataFlowStartMessage startMessage, DataFlow.Builder dataFlowBuilder) {
        dataFlowBuilder.state(RECEIVED.code());

        var responseChannelType = startMessage.getTransferType().responseChannelType();
        if (responseChannelType != null) {
            monitor.debug("PUSH dataflow with responseChannel '%s' received. Will generate data address".formatted(responseChannelType));
            var result = authorizationService.createEndpointDataReference(startMessage);

            return result.map(da -> DataFlowResponseMessage.Builder.newInstance()
                    .dataAddress(da)
                    .build());
        }
        return success(DataFlowResponseMessage.Builder.newInstance()
                .dataAddress(null)
                .build());
    }

    private boolean processReceived(DataFlow dataFlow) {
        var request = dataFlow.toRequest();
        var transferService = transferServiceRegistry.resolveTransferService(request);

        if (transferService == null) {
            dataFlow.transitToFailed("No transferService available for DataFlow " + dataFlow.getId());
            update(dataFlow);
            return true;
        }

        dataFlow.transitionToStarted(runtimeId);
        monitor.info("UPDATE dataflow %s. RuntimeId %s, UpdatedAt %s".formatted(dataFlow.getId(), dataFlow.getRuntimeId(), dataFlow.getUpdatedAt()));
        update(dataFlow);

        return entityRetryProcessFactory.doAsyncProcess(dataFlow, () -> transferService.transfer(request))
                .entityRetrieve(id -> store.findByIdAndLease(id).orElse(f -> null))
                .onSuccess((f, r) -> {
                    if (f.getState() != STARTED.code()) {
                        return;
                    }

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

    @SafeVarargs
    private Processor processDataFlowInState(DataFlowStates state, Function<DataFlow, Boolean> function, Supplier<Criterion>... additionalCriteria) {
        Supplier<Collection<DataFlow>> entitiesSupplier = () -> {
            var additional = Arrays.stream(additionalCriteria).map(Supplier::get);
            var filter = Stream.concat(Stream.of(new Criterion[]{ hasState(state.code()) }), additional)
                    .toArray(Criterion[]::new);
            return store.nextNotLeased(batchSize, filter);
        };

        return ProcessorImpl.Builder.newInstance(entitiesSupplier)
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    public static class Builder extends AbstractStateEntityManager.Builder<DataFlow, DataPlaneStore, DataPlaneManagerImpl, Builder> {

        private Builder() {
            super(new DataPlaneManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public DataPlaneManagerImpl build() {
            super.build();
            Objects.requireNonNull(manager.transferProcessClient);
            return manager;
        }

        public Builder transferServiceRegistry(TransferServiceRegistry transferServiceRegistry) {
            manager.transferServiceRegistry = transferServiceRegistry;
            return this;
        }

        public Builder transferProcessClient(TransferProcessApiClient transferProcessClient) {
            manager.transferProcessClient = transferProcessClient;
            return this;
        }

        public Builder authorizationService(DataPlaneAuthorizationService authorizationService) {
            manager.authorizationService = authorizationService;
            return this;
        }

        public Builder runtimeId(String runtimeId) {
            manager.runtimeId = runtimeId;
            return this;
        }

        public Builder flowLeaseConfiguration(FlowLeaseConfiguration flowLeaseConfiguration) {
            manager.flowLeaseConfiguration = flowLeaseConfiguration;
            return this;
        }
    }

}
