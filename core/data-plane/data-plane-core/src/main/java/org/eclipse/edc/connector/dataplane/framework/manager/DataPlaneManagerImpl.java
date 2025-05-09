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

import org.eclipse.edc.connector.dataplane.framework.DataPlaneFrameworkExtension.FlowLeaseConfiguration;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.retry.processor.Process;
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
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISIONING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISIONING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;
import static org.eclipse.edc.statemachine.retry.processor.Process.future;
import static org.eclipse.edc.statemachine.retry.processor.Process.result;

/**
 * Default data manager implementation.
 */
public class DataPlaneManagerImpl extends AbstractStateEntityManager<DataFlow, DataPlaneStore> implements DataPlaneManager {

    private DataPlaneAuthorizationService authorizationService;
    private TransferServiceRegistry transferServiceRegistry;
    private TransferProcessApiClient transferProcessClient;
    private String runtimeId;
    private FlowLeaseConfiguration flowLeaseConfiguration = new FlowLeaseConfiguration();
    private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;
    private ProvisionerManager provisionerManager;

    private DataPlaneManagerImpl() {

    }

    @Override
    public Result<Void> validate(DataFlowStartMessage dataRequest) {
        // TODO for now no validation for pull scenario, since the transfer service registry
        //      is not applicable here. Probably validation only on the source part required.
        if (PULL.equals(dataRequest.getFlowType())) {
            return success();
        } else {
            var transferService = transferServiceRegistry.resolveTransferService(dataRequest);
            return transferService != null ?
                    transferService.validate(dataRequest) :
                    Result.failure(format("Cannot find a transfer Service that can handle %s source and %s destination",
                            dataRequest.getSourceDataAddress().getType(), dataRequest.getDestinationDataAddress().getType()));
        }
    }

    @Override
    public Result<DataFlowResponseMessage> provision(DataFlowProvisionMessage message) {
        var dataFlow = DataFlow.Builder.newInstance()
                .id(message.getProcessId())
                .destination(message.getDestination())
                .callbackAddress(message.getCallbackAddress())
                .traceContext(telemetry.getCurrentTraceContext())
                .properties(message.getProperties())
                .transferType(message.getTransferType())
                .runtimeId(runtimeId)
                .build();

        var resources = resourceDefinitionGeneratorManager.generateConsumerResourceDefinition(dataFlow);
        if (resources.isEmpty()) {
            dataFlow.transitToNotified();
        } else {
            dataFlow.addResourceDefinitions(resources);
            dataFlow.transitionToProvisioning();
        }

        update(dataFlow);

        return Result.success(DataFlowResponseMessage.Builder.newInstance()
                .provisioning(!resources.isEmpty())
                .build());
    }

    @Override
    public Result<DataFlowResponseMessage> start(DataFlowStartMessage startMessage) {
        var dataFlow = DataFlow.Builder.newInstance()
                .id(startMessage.getProcessId())
                .source(startMessage.getSourceDataAddress())
                .destination(startMessage.getDestinationDataAddress())
                .callbackAddress(startMessage.getCallbackAddress())
                .traceContext(telemetry.getCurrentTraceContext())
                .properties(startMessage.getProperties())
                .transferType(startMessage.getTransferType())
                .runtimeId(runtimeId)
                .build();

        var response = switch (startMessage.getFlowType()) {
            case PULL -> handlePull(startMessage);
            case PUSH -> handlePush(startMessage);
        };

        return response.onSuccess(m -> start(dataFlow));
    }

    @Override
    public DataFlowStates getTransferState(String processId) {
        return Optional.ofNullable(store.findById(processId)).map(StatefulEntity::getState)
                .map(DataFlowStates::from).orElse(null);
    }

    @Override
    public StatusResult<Void> suspend(String dataFlowId) {
        return store.findByIdAndLease(dataFlowId)
                .flatMap(StatusResult::from)
                .compose(this::stop)
                .onSuccess(dataFlow -> {
                    dataFlow.transitToSuspended();
                    update(dataFlow);
                })
                .mapEmpty();
    }

    @Override
    public StatusResult<Void> terminate(String dataFlowId, @Nullable String reason) {
        return store.findByIdAndLease(dataFlowId)
                .flatMap(StatusResult::from)
                .compose(dataFlow -> {
                    if (dataFlow.getState() == PROVISIONED.code()) {
                        dataFlow.transitionToDeprovisioning();
                        update(dataFlow);
                        return StatusResult.success();
                    }
                    return stop(dataFlow, reason)
                            .onSuccess(flow -> {
                                flow.transitToTerminated(reason);
                                update(dataFlow);
                            })
                            .mapEmpty();
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
                .processor(processDataFlowInState(PROVISIONING, this::processProvisioning))
                .processor(processDataFlowInState(STARTED, this::updateFlowLease, ownedByThisRuntime, flowLeaseNeedsToBeUpdated))
                .processor(processDataFlowInState(STARTED, this::restartFlow, ownedByAnotherRuntime, danglingTransfer))
                .processor(processDataFlowInState(RECEIVED, this::processReceived))
                .processor(processDataFlowInState(COMPLETED, this::processCompleted))
                .processor(processDataFlowInState(FAILED, this::processFailed))
                .processor(processDataFlowInState(DEPROVISIONING, this::processDeprovisioning));
    }

    private void start(DataFlow dataFlow) {
        if (dataFlow.getTransferType().flowType() == PULL) {
            dataFlow.transitionToStarted(runtimeId);
        } else if (dataFlow.getTransferType().flowType() == PUSH) {
            dataFlow.transitToReceived(runtimeId);
        }

        update(dataFlow);
    }

    private boolean updateFlowLease(DataFlow dataFlow) {
        dataFlow.transitToReceived(runtimeId);
        dataFlow.transitionToStarted(runtimeId);
        store.save(dataFlow);
        return true;
    }

    private boolean restartFlow(DataFlow dataFlow) {
        monitor.debug("Restarting interrupted flow %s, it was owned by runtime %s".formatted(dataFlow.getId(), dataFlow.getRuntimeId()));
        start(dataFlow);
        return true;
    }

    private StatusResult<DataFlow> stop(DataFlow dataFlow) {
        return stop(dataFlow, null);
    }

    private StatusResult<DataFlow> stop(DataFlow dataFlow, String reason) {
        if (FlowType.PUSH.equals(dataFlow.getTransferType().flowType())) {
            var transferService = transferServiceRegistry.resolveTransferService(dataFlow.toRequest());

            if (transferService == null) {
                return StatusResult.failure(FATAL_ERROR, "TransferService cannot be resolved for DataFlow %s".formatted(dataFlow.getId()));
            }

            var terminateResult = transferService.terminate(dataFlow);
            if (terminateResult.failed()) {
                if (terminateResult.reason().equals(StreamFailure.Reason.NOT_FOUND)) {
                    monitor.warning("No source was found for DataFlow '%s'. This may indicate an inconsistent state.".formatted(dataFlow.getId()));
                } else {
                    return StatusResult.failure(FATAL_ERROR, "DataFlow %s cannot be terminated: %s".formatted(dataFlow.getId(), terminateResult.getFailureDetail()));
                }
            }
        } else {
            var revokeResult = authorizationService.revokeEndpointDataReference(dataFlow.getId(), reason);
            if (revokeResult.failed()) {
                return StatusResult.failure(FATAL_ERROR, "DataFlow %s cannot be terminated: %s".formatted(dataFlow.getId(), revokeResult.getFailureDetail()));
            }
        }

        return StatusResult.success(dataFlow);
    }

    private Result<DataFlowResponseMessage> handlePull(DataFlowStartMessage startMessage) {
        return authorizationService.createEndpointDataReference(startMessage)
                .onFailure(f -> monitor.warning("Error obtaining EDR DataAddress: %s".formatted(f.getFailureDetail())))
                .map(dataAddress -> DataFlowResponseMessage.Builder.newInstance()
                        .dataAddress(dataAddress)
                        .build());
    }

    private Result<DataFlowResponseMessage> handlePush(DataFlowStartMessage startMessage) {
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

    private boolean processProvisioning(DataFlow dataFlow) {
        return entityRetryProcessFactory.retryProcessor(dataFlow)
                .doProcess(future("provisioning", (flow, e) -> provisionerManager.provision(flow.getResourceDefinitions())))
                .onSuccess((flow, results) -> {
                    var newAddress = results.stream().map(AbstractResult::getContent)
                            .map(ProvisionedResource::getDataAddress)
                            .filter(Objects::nonNull)
                            .findFirst().orElse(null);
                    transferProcessClient.provisioned(dataFlow.getId(), newAddress);
                    dataFlow.transitionToProvisioned();
                    update(dataFlow);
                })
                .onFailure((flow, t) -> {
                    flow.transitionToProvisioning();
                    update(dataFlow);
                })
                .onFinalFailure((flow, e) -> {
                    flow.transitToFailed("Cannot provision: " + e.getMessage());
                    update(dataFlow);
                })
                .execute();
    }

    private boolean processDeprovisioning(DataFlow dataFlow) {
        return entityRetryProcessFactory.retryProcessor(dataFlow)
                .doProcess(future("deprovisioning", (flow, e) -> provisionerManager.deprovision(flow.getResourceDefinitions())))
                .onSuccess((flow, results) -> {
                    flow.transitionToDeprovisioned();
                    update(flow);
                })
                .onFailure((flow, t) -> {
                    flow.transitionToDeprovisioning();
                    update(flow);
                })
                .onFinalFailure((flow, t) -> {
                    flow.transitionToDeprovisionFailed();
                    update(flow);
                })
                .execute();
    }

    private boolean processReceived(DataFlow dataFlow) {
        var request = dataFlow.toRequest();
        var transferService = transferServiceRegistry.resolveTransferService(request);

        if (transferService == null) {
            dataFlow.transitToFailed("No transferService available for DataFlow " + dataFlow.getId());
            update(dataFlow);
            return true;
        }

        return entityRetryProcessFactory.retryProcessor(dataFlow)
                .doProcess(result("Validate data flow", (d, v) -> transferService.validate(request)))
                .onSuccess((flow, v) -> {
                    transferService.transfer(request)
                            .whenComplete((result, throwable) -> onTransferCompletion(result, throwable, dataFlow.getId()));

                    flow.transitionToStarted(runtimeId);
                    update(flow);
                })
                .onFailure((f, t) -> {
                    f.transitToReceived(runtimeId);
                    update(f);
                })
                .onFinalFailure((f, t) -> {
                    f.transitToFailed(t.getMessage());
                    update(f);
                })
                .execute();
    }

    private void onTransferCompletion(StreamResult<Object> result, Throwable throwable, String id) {
        store.findByIdAndLease(id)
                .onSuccess(dataFlow -> {
                    if (dataFlow.getState() != STARTED.code()) {
                        return;
                    }

                    if (throwable != null) {
                        dataFlow.transitToFailed("Unexpected exception: " + throwable.getMessage());
                    } else {
                        if (result.succeeded()) {
                            dataFlow.transitToCompleted();
                        } else {
                            dataFlow.transitToFailed(result.getFailureDetail());
                        }
                    }

                    update(dataFlow);
                });
    }

    private boolean processCompleted(DataFlow dataFlow) {
        return entityRetryProcessFactory.retryProcessor(dataFlow)
                .doProcess(Process.result("Complete data flow", (d, v) -> transferProcessClient.completed(dataFlow.toRequest())))
                .onSuccess((d, v) -> {
                    dataFlow.transitToNotified();
                    update(dataFlow);
                })
                .onFailure((d, t) -> {
                    dataFlow.transitToCompleted();
                    update(dataFlow);
                })
                .onFinalFailure((d, t) -> {
                    dataFlow.transitToTerminated(t.getMessage());
                    update(dataFlow);
                })
                .execute();
    }

    private boolean processFailed(DataFlow dataFlow) {
        return entityRetryProcessFactory.retryProcessor(dataFlow)
                .doProcess(Process.result("Fail data flow", (d, v) -> transferProcessClient.failed(dataFlow.toRequest(), dataFlow.getErrorDetail())))
                .onSuccess((d, v) -> {
                    dataFlow.transitToNotified();
                    update(dataFlow);
                })
                .onFailure((d, t) -> {
                    dataFlow.transitToFailed(dataFlow.getErrorDetail());
                    update(dataFlow);
                })
                .onFinalFailure((d, t) -> {
                    dataFlow.transitToTerminated(t.getMessage());
                    update(dataFlow);
                })
                .execute();
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

        public Builder resourceDefinitionGeneratorManager(ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager) {
            manager.resourceDefinitionGeneratorManager = resourceDefinitionGeneratorManager;
            return this;
        }

        public Builder provisionerManager(ProvisionerManager provisionerManager) {
            manager.provisionerManager = provisionerManager;
            return this;
        }
    }

}
