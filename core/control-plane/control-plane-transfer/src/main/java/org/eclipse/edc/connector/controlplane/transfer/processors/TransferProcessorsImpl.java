/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.processors;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.statemachine.retry.processor.Process.futureResult;
import static org.eclipse.edc.statemachine.retry.processor.Process.result;

/**
 * Define all the business logic needed from the manager.
 */
public class TransferProcessorsImpl implements TransferProcessors {

    private final PolicyArchive policyArchive;
    private final EntityRetryProcessFactory entityRetryProcessFactory;
    private final DataFlowController dataFlowController;
    private final DataAddressStore dataAddressStore;
    private final TransferProcessObservable observable;
    private final TransferProcessStore store;
    private final Monitor monitor;
    private final DataAddressResolver addressResolver;
    private final ProtocolWebhookResolver protocolWebhookResolver;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public TransferProcessorsImpl(PolicyArchive policyArchive, EntityRetryProcessFactory entityRetryProcessFactory,
                                  DataFlowController dataFlowController, DataAddressStore dataAddressStore,
                                  TransferProcessObservable observable, TransferProcessStore store, Monitor monitor,
                                  DataAddressResolver addressResolver,
                                  ProtocolWebhookResolver protocolWebhookResolver,
                                  RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.policyArchive = policyArchive;
        this.entityRetryProcessFactory = entityRetryProcessFactory;
        this.dataFlowController = dataFlowController;
        this.dataAddressStore = dataAddressStore;
        this.observable = observable;
        this.store = store;
        this.monitor = monitor;
        this.addressResolver = addressResolver;
        this.protocolWebhookResolver = protocolWebhookResolver;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processConsumerInitial(TransferProcess process) {
        var contractId = process.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            var message = "Policy not found for contract: " + contractId;
            transitionToTerminated(process, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("prepare data flow", (t, ignored) -> dataFlowController.prepare(process, policy)))
                .doProcess(result("store eventual data address", (t, response) -> {
                    if (response.getDataAddress() == null) {
                        return StatusResult.success(response);
                    }
                    return dataAddressStore.store(response.getDataAddress(), process).flatMap(StatusResult::from)
                            .map(it -> response);
                }))
                .onSuccess((t, response) -> {
                    process.setDataPlaneId(response.getDataPlaneId());
                    if (response.isAsync()) {
                        process.transitionPreparationRequested();
                        observable.invokeForEach(l -> l.preparationRequested(process));
                    } else {
                        process.transitionRequesting();
                    }
                    update(process);
                })
                .onFailure((t, throwable) -> transitionToInitial(t))
                .onFinalFailure(this::transitionToTerminated)
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processProviderInitial(TransferProcess process) {
        var contractId = process.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            var message = "Policy not found for contract: " + contractId;
            transitionToTerminated(process, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        eventuallySetContentDataAddress(process);

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("start data flow", (t, ignored) -> dataFlowController.start(process, policy)))
                .doProcess(result("eventually store data address", (t, response) -> {
                    process.setDataPlaneId(response.getDataPlaneId());
                    if (response.isAsync()) {
                        process.transitionStartupRequested();
                    } else {
                        process.transitionStarting();

                        var dataPlaneDataAddress = response.getDataAddress();
                        if (dataPlaneDataAddress != null) {
                            return dataAddressStore.store(dataPlaneDataAddress, process).flatMap(StatusResult::from);
                        }
                    }

                    return StatusResult.success();
                }))
                .onSuccess((t, response) -> update(t))
                .onFailure((t, throwable) -> transitionToInitial(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();

    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processRequesting(TransferProcess process) {
        var callbackAddress = protocolWebhookResolver.getWebhook(process.getParticipantContextId(), process.getProtocol());

        if (callbackAddress == null) {
            var message = "No callback address found for protocol: " + process.getProtocol();
            transitionToTerminated(process, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        var agreementId = policyArchive.getAgreementIdForContract(process.getContractId());

        if (agreementId == null) {
            var message = "No agreement found for contract: " + process.getContractId();
            transitionToTerminated(process, message);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        var dataAddress = dataAddressStore.resolve(process).orElse(f -> null);

        var messageBuilder = TransferRequestMessage.Builder.newInstance()
                .callbackAddress(callbackAddress.url())
                .dataAddress(dataAddress)
                .transferType(process.getTransferType())
                .contractId(agreementId);

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(futureResult("Dispatch TransferRequestMessage to " + process.getCounterPartyAddress(),
                        (t, c) -> dispatch(messageBuilder, t, TransferProcessAck.class))
                )
                .onSuccess(this::transitionToRequested)
                .onFailure((t, throwable) -> transitionToRequesting(t))
                .onFinalFailure(this::transitionToTerminated)
                .execute();

    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processStartupRequested(TransferProcess process) {
        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Notify started to data plane " + process.getCounterPartyAddress(), (t, r) ->
                        dataFlowController.started(process))
                )
                .onSuccess((t, c) -> transitionToStarted(t))
                .onFailure((t, throwable) -> transitionToStartupRequested(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processStarting(TransferProcess process) {
        Function<RetryProcessor<TransferProcess, ?>, RetryProcessor<TransferProcess, DataAddress>> preProcessing = r -> r
                .doProcess(result("resolve data address", (p, ignored) -> StatusResult.success(dataAddressStore.resolve(p).orElse(f -> null))));

        return sendStartMessage(process, this::transitionToStarting, preProcessing);
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processProviderResuming(TransferProcess process) {
        var policy = policyArchive.findPolicyForContract(process.getContractId());

        Function<RetryProcessor<TransferProcess, ?>, RetryProcessor<TransferProcess, DataAddress>> preProcess = r -> r
                .doProcess(result("Data Plane resume", (t, ignored) -> dataFlowController.start(process, policy)))
                .doProcess(result("Forward DataAddress", (t, response) -> StatusResult.success(response.getDataAddress())));

        return sendStartMessage(process, this::transitionToResuming, preProcess);
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processConsumerResuming(TransferProcess process) {
        var messageBuilder = TransferStartMessage.Builder.newInstance();

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(futureResult("Dispatch TransferStartMessage for transfer resume to " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> dispatch(messageBuilder, t, Object.class))
                )
                .onSuccess((t, c) -> transitionToResumed(t))
                .onFailure((t, throwable) -> transitionToResuming(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processCompleting(TransferProcess process) {
        var builder = TransferCompletionMessage.Builder.newInstance();

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(futureResult("Dispatch TransferCompletionMessage to " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> {
                            if (t.completionWasRequestedByCounterParty()) {
                                var result = dataFlowController.completed(t);
                                return completedFuture(result.mapEmpty());
                            } else {
                                return dispatch(builder, t, Object.class);
                            }
                        })
                )
                .onSuccess((t, c) -> transitionToCompleted(t))
                .onFailure((t, throwable) -> transitionToCompleting(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processSuspending(TransferProcess process) {
        var builder = TransferSuspensionMessage.Builder.newInstance()
                .reason(process.getErrorDetail());

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Suspend DataFlow", (t, c) -> dataFlowController.suspend(process)))
                .doProcess(futureResult("Dispatch TransferSuspensionMessage to " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> {
                            if (t.suspensionWasRequestedByCounterParty()) {
                                return completedFuture(StatusResult.success(null));
                            } else {
                                return dispatch(builder, t, Object.class);
                            }
                        })
                )
                .onSuccess((t, content) -> transitionToSuspended(t))
                .onFailure((t, throwable) -> {
                    if (t.suspensionWasRequestedByCounterParty()) {
                        transitionToSuspendingRequested(t, throwable.getMessage());
                    } else {
                        transitionToSuspending(t, throwable.getMessage());
                    }
                })
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> processTerminating(TransferProcess process) {
        if (process.getType() == CONSUMER && process.getState() < REQUESTED.code()) {
            transitionToTerminated(process);
            return CompletableFuture.completedFuture(StatusResult.success());
        }

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Terminate DataFlow", (p, i) -> dataFlowController.terminate(process)))
                .doProcess(futureResult("Dispatch TransferTerminationMessage", (t, n) -> {
                    if (t.terminationWasRequestedByCounterParty()) {
                        return completedFuture(StatusResult.success(null));
                    } else {
                        return dispatch(TransferTerminationMessage.Builder.newInstance().reason(t.getErrorDetail()), t, Object.class);
                    }
                }))
                .onSuccess((t, c) -> transitionToTerminated(t))
                .onFailure((t, throwable) -> {
                    if (t.terminationWasRequestedByCounterParty()) {
                        transitionToTerminatingRequested(t, throwable.getMessage());
                    } else {
                        transitionToTerminating(t, throwable.getMessage());
                    }
                })
                .onFinalFailure(this::transitionToTerminated)
                .execute();
    }

    /**
     * this is to support the legacy data plane signaling, it will be deleted when the legacy protocol will be dismissed
     *
     * @deprecated can be deleted as soon as the legacy data plane signaling protocol is dismissed.
     */
    @Deprecated(since = "0.16.0")
    private void eventuallySetContentDataAddress(TransferProcess process) {
        var assetId = process.getAssetId();
        var dataAddress = addressResolver.resolveForAsset(assetId);
        if (dataAddress != null) {
            process.setContentDataAddress(dataAddress);
        }
    }

    private CompletableFuture<StatusResult<Void>> sendStartMessage(TransferProcess process, Consumer<TransferProcess> onFailure, Function<RetryProcessor<TransferProcess, ?>, RetryProcessor<TransferProcess, DataAddress>> preProcessing) {
        return preProcessing.apply(entityRetryProcessFactory.retryProcessor(process))
                .doProcess(futureResult("Dispatch TransferRequestMessage to: " + process.getCounterPartyAddress(), (t, dataAddress) -> {
                    var messageBuilder = TransferStartMessage.Builder.newInstance().dataAddress(dataAddress);
                    return dispatch(messageBuilder, t, Object.class)
                            .thenApply((Function<StatusResult<Object>, StatusResult<DataAddress>>) i -> i.map(a -> dataAddress));
                }))
                .doProcess(result("Store eventual DataAddress", (t, dataAddress) -> {
                    if (dataAddress == null) {
                        return StatusResult.success();
                    }
                    return dataAddressStore.store(dataAddress, t).flatMap(StatusResult::from);
                }))
                .onSuccess((t, dataAddress) -> {
                    t.transitionStarted();
                    update(t);
                    observable.invokeForEach(l -> l.started(t, TransferProcessStartedData.Builder.newInstance().build()));
                })
                .onFailure((t, throwable) -> onFailure.accept(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    private <T, M extends TransferRemoteMessage, B extends TransferRemoteMessage.Builder<M, B>> CompletableFuture<StatusResult<T>> dispatch(B messageBuilder, TransferProcess process, Class<T> responseType) {

        var contractPolicy = policyArchive.findPolicyForContract(process.getContractId());

        messageBuilder.protocol(process.getProtocol())
                .counterPartyAddress(process.getCounterPartyAddress())
                .processId(Optional.ofNullable(process.getCorrelationId()).orElse(process.getId()))
                .policy(contractPolicy);

        if (process.lastSentProtocolMessage() != null) {
            messageBuilder.id(process.lastSentProtocolMessage());
        }

        if (process.getType() == PROVIDER) {
            messageBuilder.consumerPid(process.getCorrelationId())
                    .providerPid(process.getId())
                    .counterPartyId(contractPolicy.getAssignee());
        } else {
            messageBuilder.consumerPid(process.getId())
                    .providerPid(process.getCorrelationId())
                    .counterPartyId(contractPolicy.getAssigner());
        }

        var message = messageBuilder.build();

        process.lastSentProtocolMessage(message.getId());

        return dispatcherRegistry.dispatch(process.getParticipantContextId(), responseType, message);
    }

    private CompletableFuture<StatusResult<Void>> setPending(TransferProcess transferProcess) {
        transferProcess.setPending(true);
        update(transferProcess);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    private void transitionToInitial(TransferProcess process) {
        process.transitionInitial();
        update(process);
    }

    private void transitionToRequesting(TransferProcess process) {
        process.transitionRequesting();
        update(process);
    }

    private void transitionToRequested(TransferProcess transferProcess, TransferProcessAck ack) {
        transferProcess.transitionRequested();
        transferProcess.setCorrelationId(ack.getProviderPid());
        update(transferProcess);
        observable.invokeForEach(l -> l.requested(transferProcess));
    }

    private void transitionToStarting(TransferProcess transferProcess) {
        transferProcess.transitionStarting();
        update(transferProcess);
    }

    private void transitionToStarted(TransferProcess transferProcess) {
        transferProcess.transitionStarted();
        update(transferProcess);
        var transferStartedData = TransferProcessStartedData.Builder.newInstance()
                .dataAddress(transferProcess.getContentDataAddress())
                .build();
        observable.invokeForEach(l -> l.started(transferProcess, transferStartedData));
    }

    private void transitionToStartupRequested(TransferProcess transferProcess) {
        transferProcess.transitionStartupRequested();
        update(transferProcess);
    }

    private void transitionToResuming(TransferProcess process) {
        process.transitionResuming();
        update(process);
    }

    private void transitionToResumed(TransferProcess process) {
        process.transitionResumed();
        update(process);
    }

    private void transitionToCompleting(TransferProcess process) {
        process.transitionCompleting();
        update(process);
    }

    private void transitionToCompleted(TransferProcess transferProcess) {
        transferProcess.transitionCompleted();
        update(transferProcess).compose(i -> dataAddressStore.remove(transferProcess))
                .onSuccess(i -> observable.invokeForEach(l -> l.completed(transferProcess)));
    }

    private void transitionToSuspending(TransferProcess process, String message) {
        process.transitionSuspending(message);
        update(process);
    }

    private void transitionToSuspendingRequested(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionSuspendingRequested(message);
        update(process);
    }

    private void transitionToSuspended(TransferProcess process) {
        process.transitionSuspended();
        update(process);
        observable.invokeForEach(l -> l.suspended(process));
    }

    private void transitionToTerminating(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionTerminating(message);
        update(process);
    }

    private void transitionToTerminatingRequested(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionTerminatingRequested(message);
        update(process);
    }

    private void transitionToTerminated(TransferProcess process, Throwable throwable) {
        transitionToTerminated(process, throwable.getMessage());
    }

    private void transitionToTerminated(TransferProcess process, String message) {
        process.setErrorDetail(message);
        monitor.warning(message);
        transitionToTerminated(process);
    }

    private void transitionToTerminated(TransferProcess process) {
        process.transitionTerminated();
        update(process).compose(i -> dataAddressStore.remove(process))
                .onSuccess(i -> observable.invokeForEach(l -> l.terminated(process)));
    }

    private StoreResult<Void> update(TransferProcess entity) {
        return store.save(entity)
                .onSuccess(ignored -> {
                    var error = entity.getErrorDetail() == null ? "" : ". errorDetail: " + entity.getErrorDetail();

                    monitor.debug(() -> "[%s] %s %s is now in state %s%s"
                            .formatted(this.getClass().getSimpleName(), entity.getClass().getSimpleName(),
                                    entity.getId(), entity.stateAsString(), error));
                });
    }


}
