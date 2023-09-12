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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.transfer.process;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.provision.DeprovisionResponsesHandler;
import org.eclipse.edc.connector.transfer.provision.ProvisionResponsesHandler;
import org.eclipse.edc.connector.transfer.provision.ResponsesHandler;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_SEND_RETRY_LIMIT;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_SECRET;

/**
 * This transfer process manager receives a {@link TransferProcess} and transitions it through its internal state
 * machine (cf {@link TransferProcessStates}. When submitting a new {@link TransferProcess} it gets created and inserted
 * into the {@link TransferProcessStore}, then returns to the caller.
 * <p>
 * All subsequent state transitions happen asynchronously, the {@code AsyncTransferProcessManager#initiate*Request()}
 * will return immediately.
 * <p>
 * A data transfer processes transitions through a series of states, which allows the system to model both terminating
 * and non-terminating (e.g. streaming) transfers. Transitions occur asynchronously, since long-running processes such
 * as resource provisioning may need to be completed before transitioning to a subsequent state. The permissible state
 * transitions are defined by {@link TransferProcessStates}.
 * <p>
 * The transfer manager performs continual iterations, which seek to advance the state of transfer processes, including
 * recovery, in a FIFO state-based ordering. Each iteration will seek to transition a set number of processes for each
 * state to avoid situations where an excessive number of processes in one state block progress of processes in other
 * states.
 * <p>
 * If no processes need to be transitioned, the transfer manager will wait according to the defined {@link WaitStrategy}
 * before conducting the next iteration. A wait strategy may implement a backoff scheme.
 */
public class TransferProcessManagerImpl implements TransferProcessManager {
    private int batchSize = DEFAULT_BATCH_SIZE;
    private WaitStrategy waitStrategy = () -> DEFAULT_ITERATION_WAIT;
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private StatusCheckerRegistry statusCheckerRegistry;
    private Vault vault;
    private TransferProcessObservable observable;
    private Monitor monitor;
    private Telemetry telemetry;
    private ExecutorInstrumentation executorInstrumentation;
    private StateMachineManager stateMachineManager;
    private DataAddressResolver addressResolver;
    private PolicyArchive policyArchive;
    private EntityRetryProcessFactory entityRetryProcessFactory;
    private Clock clock;
    private EntityRetryProcessConfiguration entityRetryProcessConfiguration = defaultEntityRetryProcessConfiguration();
    private ProtocolWebhook protocolWebhook;
    private ProvisionResponsesHandler provisionResponsesHandler;
    private DeprovisionResponsesHandler deprovisionResponsesHandler;
    private TransferProcessPendingGuard pendingGuard = tp -> false;

    private TransferProcessManagerImpl() {
    }

    public void start() {
        entityRetryProcessFactory = new EntityRetryProcessFactory(monitor, clock, entityRetryProcessConfiguration);
        stateMachineManager = StateMachineManager.Builder.newInstance("transfer-process", monitor, executorInstrumentation, waitStrategy)
                .processor(processTransfersInState(INITIAL, this::processInitial))
                .processor(processTransfersInState(PROVISIONING, this::processProvisioning))
                .processor(processTransfersInState(PROVISIONED, this::processProvisioned))
                .processor(processConsumerTransfersInState(REQUESTING, this::processRequesting))
                .processor(processProviderTransfersInState(STARTING, this::processStarting))
                .processor(processConsumerTransfersInState(STARTED, this::processStarted))
                .processor(processTransfersInState(COMPLETING, this::processCompleting))
                .processor(processTransfersInState(TERMINATING, this::processTerminating))
                .processor(processTransfersInState(DEPROVISIONING, this::processDeprovisioning))
                .build();
        stateMachineManager.start();
    }

    public void stop() {
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    /**
     * Initiate a consumer request TransferProcess.
     */
    @WithSpan
    @Override
    public StatusResult<TransferProcess> initiateConsumerRequest(TransferRequest transferRequest) {
        // make the request idempotent: if the process exists, return
        var id = Optional.ofNullable(transferRequest.getId()).orElseGet(() -> UUID.randomUUID().toString());
        var existingTransferProcess = transferProcessStore.findForCorrelationId(id);
        if (existingTransferProcess != null) {
            return StatusResult.success(existingTransferProcess);
        }
        var dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .assetId(transferRequest.getAssetId())
                .connectorId(transferRequest.getConnectorId())
                .dataDestination(transferRequest.getDataDestination())
                .connectorAddress(transferRequest.getConnectorAddress())
                .contractId(transferRequest.getContractId())
                .destinationType(transferRequest.getDataDestination().getType())
                .protocol(transferRequest.getProtocol())
                .dataDestination(transferRequest.getDataDestination())
                .build();

        var process = TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(dataRequest)
                .type(CONSUMER)
                .clock(clock)
                .privateProperties(transferRequest.getPrivateProperties())
                .callbackAddresses(transferRequest.getCallbackAddresses())
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        observable.invokeForEach(l -> l.preCreated(process));
        update(process);
        observable.invokeForEach(l -> l.initiated(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));

        return StatusResult.success(process);
    }

    /**
     * Process INITIAL transfer<p> set it to PROVISIONING
     *
     * @param process the INITIAL transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processInitial(TransferProcess process) {
        var contractId = process.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            transitionToTerminated(process, "Policy not found for contract: " + contractId);
            return true;
        }

        ResourceManifest manifest;
        if (process.getType() == CONSUMER) {
            var manifestResult = manifestGenerator.generateConsumerResourceManifest(process.getDataRequest(), policy);
            if (manifestResult.failed()) {
                transitionToTerminated(process, format("Resource manifest for process %s cannot be modified to fulfil policy. %s", process.getId(), manifestResult.getFailureMessages()));
                return true;
            }
            manifest = manifestResult.getContent();
        } else {
            var assetId = process.getAssetId();
            var dataAddress = addressResolver.resolveForAsset(assetId);
            if (dataAddress == null) {
                transitionToTerminating(process, "Asset not found: " + assetId);
                return true;
            }
            // default the content address to the asset address; this may be overridden during provisioning
            process.setContentDataAddress(dataAddress);

            var dataDestination = process.getDataDestination();
            var secret = dataDestination.getStringProperty(EDC_DATA_ADDRESS_SECRET);
            if (secret != null) {
                vault.storeSecret(dataDestination.getKeyName(), secret);
            }

            manifest = manifestGenerator.generateProviderResourceManifest(process.getDataRequest(), dataAddress, policy);
        }

        process.transitionProvisioning(manifest);
        observable.invokeForEach(l -> l.preProvisioning(process));
        update(process);
        return true;
    }

    /**
     * Process PROVISIONING transfer<p> Launch provision process. On completion, set to PROVISIONED if succeeded, TERMINATED
     * otherwise
     * <p>
     * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a
     * provider, provisioning is initiated when a request is received and map involve preprocessing data or other
     * operations.
     *
     * @param process the PROVISIONING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProvisioning(TransferProcess process) {
        var policy = policyArchive.findPolicyForContract(process.getContractId());

        var resources = process.getResourcesToProvision();

        return entityRetryProcessFactory.doAsyncProcess(process, () -> provisionManager.provision(resources, policy))
                .entityRetrieve(transferProcessStore::findById)
                .onSuccess((transferProcess, responses) -> handleResult(transferProcess, responses, provisionResponsesHandler))
                .onFailure((t, throwable) -> transitionToProvisioning(t))
                .onRetryExhausted((t, throwable) -> {
                    if (t.getType() == PROVIDER) {
                        transitionToTerminating(t, format("Error during provisioning: %s", throwable.getMessage()));
                    } else {
                        transitionToTerminated(t, format("Error during provisioning: %s", throwable.getMessage()));
                    }
                })
                .execute("Provisioning");
    }

    /**
     * Process PROVISIONED transfer<p> If CONSUMER, set it to REQUESTING, if PROVIDER set to STARTING
     *
     * @param process the PROVISIONED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProvisioned(TransferProcess process) {
        if (CONSUMER == process.getType()) {
            transitionToRequesting(process);
        } else {
            transitionToStarting(process);
        }
        return true;
    }

    /**
     * Process REQUESTING transfer<p> If CONSUMER, send request to the provider, should never be PROVIDER
     *
     * @param process the REQUESTING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processRequesting(TransferProcess process) {
        var originalDestination = process.getDataDestination();
        var dataDestination = Optional.ofNullable(originalDestination.getKeyName())
                .flatMap(key -> Optional.ofNullable(vault.resolveSecret(key)))
                .map(secret -> DataAddress.Builder.newInstance().properties(originalDestination.getProperties()).property(EDC_DATA_ADDRESS_SECRET, secret).build())
                .orElse(originalDestination);

        var message = TransferRequestMessage.Builder.newInstance()
                .processId(process.getCorrelationId())
                .protocol(process.getProtocol())
                .counterPartyAddress(process.getConnectorAddress())
                .callbackAddress(protocolWebhook.url())
                .dataDestination(dataDestination)
                .contractId(process.getContractId())
                .policy(policyArchive.findPolicyForContract(process.getContractId()))
                .build();

        var description = format("Send %s to %s", message.getClass().getSimpleName(), message.getCounterPartyAddress());
        return entityRetryProcessFactory.doAsyncStatusResultProcess(process, () -> dispatcherRegistry.dispatch(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToRequested(t))
                .onRetryExhausted(this::transitionToTerminated)
                .onFailure((t, throwable) -> transitionToRequesting(t))
                .onFatalError((n, failure) -> transitionToTerminated(n, failure.getFailureDetail()))
                .execute(description);
    }

    /**
     * Process STARTING transfer<p> If PROVIDER, starts data transfer and send message to consumer, should never be CONSUMER
     *
     * @param process the STARTING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStarting(TransferProcess process) {
        var policy = policyArchive.findPolicyForContract(process.getContractId());

        var description = "Initiate data flow";

        return entityRetryProcessFactory.doSyncProcess(process, () -> dataFlowManager.initiate(process, policy))
                .onSuccess((p, dataFlowResponse) -> sendTransferStartMessage(p, dataFlowResponse, policy))
                .onFatalError((p, failure) -> transitionToTerminating(p, failure.getFailureDetail()))
                .onFailure((t, failure) -> transitionToStarting(t))
                .onRetryExhausted((p, failure) -> transitionToTerminating(p, failure.getFailureDetail()))
                .execute(description);
    }

    @WithSpan
    private void sendTransferStartMessage(TransferProcess process, DataFlowResponse dataFlowResponse, Policy policy) {
        var message = TransferStartMessage.Builder.newInstance()
                .processId(process.getCorrelationId())
                .protocol(process.getProtocol())
                .dataAddress(dataFlowResponse.getDataAddress())
                .counterPartyAddress(process.getConnectorAddress())
                .policy(policy)
                .build();

        var description = format("Send %s to %s", message.getClass().getSimpleName(), process.getConnectorAddress());

        entityRetryProcessFactory.doAsyncStatusResultProcess(process, () -> dispatcherRegistry.dispatch(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToStarted(t))
                .onFailure((t, throwable) -> transitionToStarting(t))
                .onFatalError((n, failure) -> transitionToTerminated(n, failure.getFailureDetail()))
                .onRetryExhausted((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute(description);
    }

    /**
     * Process STARTED transfer<p> if is completed or there's no checker and it's not managed, set to COMPLETE,
     * nothing otherwise.
     *
     * @param transferProcess the STARTED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStarted(TransferProcess transferProcess) {
        return entityRetryProcessFactory.doSimpleProcess(transferProcess, () -> checkCompletion(transferProcess))
                .execute("Check completion");
    }

    @NotNull
    private Boolean checkCompletion(TransferProcess transferProcess) {
        var checker = statusCheckerRegistry.resolve(transferProcess.getDataDestination().getType());
        if (checker == null) {
            monitor.warning(format("No checker found for process %s. The process will not advance to the COMPLETED state.", transferProcess.getId()));
            return false;
        } else {
            var resources = transferProcess.getProvisionedResources();
            if (checker.isComplete(transferProcess, resources)) {
                transitionToCompleting(transferProcess);
                return true;
            } else {
                monitor.debug(format("Transfer process %s not COMPLETED yet. The process will stay in STARTED.", transferProcess.getId()));
                return false;
            }
        }
    }

    /**
     * Process COMPLETING transfer<p> Send COMPLETED message to counter-part
     *
     * @param process the COMPLETING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processCompleting(TransferProcess process) {
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol(process.getProtocol())
                .counterPartyAddress(process.getConnectorAddress())
                .processId(process.getCorrelationId())
                .policy(policyArchive.findPolicyForContract(process.getContractId()))
                .build();

        var description = format("Send %s to %s", message.getClass().getSimpleName(), process.getConnectorAddress());
        return entityRetryProcessFactory.doAsyncStatusResultProcess(process, () -> dispatcherRegistry.dispatch(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToCompleted(t))
                .onFailure((t, throwable) -> transitionToCompleting(t))
                .onFatalError((n, failure) -> transitionToTerminated(n, failure.getFailureDetail()))
                .onRetryExhausted((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute(description);
    }

    /**
     * Process TERMINATING transfer<p> Send TERMINATED message to counter-part, unless it is CONSUMER and not yet
     * REQUESTED, because in that case the counter-part does know nothing about the transfer yet
     *
     * @param process the TERMINATING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processTerminating(TransferProcess process) {
        if (process.getType() == CONSUMER && process.getState() < REQUESTED.code()) {
            transitionToTerminated(process);
            return true;
        }

        var message = TransferTerminationMessage.Builder.newInstance()
                .counterPartyAddress(process.getConnectorAddress())
                .protocol(process.getProtocol())
                .processId(process.getCorrelationId())
                .policy(policyArchive.findPolicyForContract(process.getContractId()))
                .build();

        var description = format("Send %s to %s", message.getClass().getSimpleName(), process.getConnectorAddress());
        return entityRetryProcessFactory.doAsyncStatusResultProcess(process, () -> dispatcherRegistry.dispatch(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToTerminated(t))
                .onFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .onFatalError((n, failure) -> transitionToTerminated(n, failure.getFailureDetail()))
                .onRetryExhausted(this::transitionToTerminated)
                .execute(description);
    }

    /**
     * Process DEPROVISIONING transfer<p> Launch deprovision process. On completion, set to DEPROVISIONED if succeeded,
     * DEPROVISIONED otherwise
     *
     * @param process the DEPROVISIONING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processDeprovisioning(TransferProcess process) {
        observable.invokeForEach(l -> l.preDeprovisioning(process)); // TODO: this is called here since it's not callable from the command handler

        var policy = policyArchive.findPolicyForContract(process.getContractId());

        var resourcesToDeprovision = process.getResourcesToDeprovision();

        return entityRetryProcessFactory.doAsyncProcess(process, () -> provisionManager.deprovision(resourcesToDeprovision, policy))
                .entityRetrieve(transferProcessStore::findById)
                .onSuccess((transferProcess, responses) -> handleResult(transferProcess, responses, deprovisionResponsesHandler))
                .onFailure((t, throwable) -> transitionToDeprovisioning(t))
                .onRetryExhausted((t, throwable) -> transitionToDeprovisioningError(t, throwable.getMessage()))
                .execute("deprovisioning");
    }

    private <T> void handleResult(TransferProcess transferProcess, List<StatusResult<T>> responses, ResponsesHandler<StatusResult<T>> handler) {
        if (handler.handle(transferProcess, responses)) {
            update(transferProcess);
            handler.postActions(transferProcess);
        } else {
            breakLease(transferProcess);
        }
    }

    private Processor processConsumerTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending(), Criterion.criterion("type", "=", CONSUMER.name()) };
        return createProcessor(function, filter);
    }

    private Processor processProviderTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending(), Criterion.criterion("type", "=", PROVIDER.name()) };
        return createProcessor(function, filter);
    }

    private Processor processTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending() };
        return createProcessor(function, filter);
    }

    private ProcessorImpl<TransferProcess> createProcessor(Function<TransferProcess, Boolean> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> transferProcessStore.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .guard(pendingGuard, this::setPending)
                .onNotProcessed(this::breakLease)
                .build();
    }

    private boolean setPending(TransferProcess transferProcess) {
        transferProcess.setPending(true);
        update(transferProcess);
        return true;
    }

    private void transitionToProvisioning(TransferProcess process) {
        process.transitionProvisioning(process.getResourceManifest());
        observable.invokeForEach(l -> l.preProvisioning(process));
        update(process);
    }

    private void transitionToRequesting(TransferProcess process) {
        process.transitionRequesting();
        observable.invokeForEach(l -> l.preRequesting(process));
        update(process);
    }

    private void transitionToRequested(TransferProcess transferProcess) {
        transferProcess.transitionRequested();
        observable.invokeForEach(l -> l.preRequested(transferProcess));
        update(transferProcess);
        observable.invokeForEach(l -> l.requested(transferProcess));
    }

    private void transitionToStarting(TransferProcess transferProcess) {
        transferProcess.transitionStarting();
        update(transferProcess);
    }

    private void transitionToStarted(TransferProcess process) {
        process.transitionStarted();
        observable.invokeForEach(l -> l.preStarted(process));
        update(process);
        observable.invokeForEach(l -> l.started(process, TransferProcessStartedData.Builder.newInstance().build()));
    }

    private void transitionToCompleting(TransferProcess process) {
        process.transitionCompleting();
        update(process);
    }

    private void transitionToCompleted(TransferProcess transferProcess) {
        transferProcess.transitionCompleted();
        observable.invokeForEach(l -> l.preCompleted(transferProcess));
        update(transferProcess);
        observable.invokeForEach(l -> l.completed(transferProcess));
    }

    private void transitionToTerminating(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionTerminating(message);
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
        observable.invokeForEach(l -> l.preTerminated(process));
        update(process);
        observable.invokeForEach(l -> l.terminated(process));
    }

    private void transitionToDeprovisioning(TransferProcess process) {
        process.transitionDeprovisioning();
        update(process);
    }

    private void transitionToDeprovisioningError(TransferProcess transferProcess, String message) {
        monitor.severe(message);
        transferProcess.transitionDeprovisioned(message);
        observable.invokeForEach(l -> l.preDeprovisioned(transferProcess));
        update(transferProcess);
        observable.invokeForEach(l -> l.deprovisioned(transferProcess));
    }

    private void update(TransferProcess transferProcess) {
        transferProcessStore.save(transferProcess);
        monitor.debug(format("TransferProcess %s is now in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
    }

    private void breakLease(TransferProcess process) {
        transferProcessStore.save(process);
    }

    @NotNull
    private EntityRetryProcessConfiguration defaultEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(DEFAULT_SEND_RETRY_LIMIT, () -> new ExponentialWaitStrategy(DEFAULT_SEND_RETRY_BASE_DELAY));
    }

    public static class Builder {
        private final TransferProcessManagerImpl manager;

        private Builder() {
            manager = new TransferProcessManagerImpl();
            manager.clock = Clock.systemUTC(); // default implementation
            manager.telemetry = new Telemetry(); // default noop implementation
            manager.executorInstrumentation = ExecutorInstrumentation.noop(); // default noop implementation
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

        public Builder manifestGenerator(ResourceManifestGenerator manifestGenerator) {
            manager.manifestGenerator = manifestGenerator;
            return this;
        }

        public Builder provisionManager(ProvisionManager provisionManager) {
            manager.provisionManager = provisionManager;
            return this;
        }

        public Builder dataFlowManager(DataFlowManager dataFlowManager) {
            manager.dataFlowManager = dataFlowManager;
            return this;
        }

        public Builder dispatcherRegistry(RemoteMessageDispatcherRegistry registry) {
            manager.dispatcherRegistry = registry;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder statusCheckerRegistry(StatusCheckerRegistry statusCheckerRegistry) {
            manager.statusCheckerRegistry = statusCheckerRegistry;
            return this;
        }

        public Builder telemetry(Telemetry telemetry) {
            manager.telemetry = telemetry;
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

        public Builder vault(Vault vault) {
            manager.vault = vault;
            return this;
        }

        public Builder observable(TransferProcessObservable observable) {
            manager.observable = observable;
            return this;
        }

        public Builder transferProcessStore(TransferProcessStore transferProcessStore) {
            manager.transferProcessStore = transferProcessStore;
            return this;
        }

        public Builder policyArchive(PolicyArchive policyArchive) {
            manager.policyArchive = policyArchive;
            return this;
        }

        public Builder addressResolver(DataAddressResolver addressResolver) {
            manager.addressResolver = addressResolver;
            return this;
        }

        public Builder entityRetryProcessConfiguration(EntityRetryProcessConfiguration entityRetryProcessConfiguration) {
            manager.entityRetryProcessConfiguration = entityRetryProcessConfiguration;
            return this;
        }

        public Builder protocolWebhook(ProtocolWebhook protocolWebhook) {
            manager.protocolWebhook = protocolWebhook;
            return this;
        }

        public Builder provisionResponsesHandler(ProvisionResponsesHandler provisionResponsesHandler) {
            manager.provisionResponsesHandler = provisionResponsesHandler;
            return this;
        }

        public Builder deprovisionResponsesHandler(DeprovisionResponsesHandler deprovisionResponsesHandler) {
            manager.deprovisionResponsesHandler = deprovisionResponsesHandler;
            return this;
        }

        public Builder pendingGuard(TransferProcessPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }

        public TransferProcessManagerImpl build() {
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator cannot be null");
            Objects.requireNonNull(manager.provisionManager, "provisionManager cannot be null");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager cannot be null");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry cannot be null");
            Objects.requireNonNull(manager.monitor, "monitor cannot be null");
            Objects.requireNonNull(manager.executorInstrumentation, "executorInstrumentation cannot be null");
            Objects.requireNonNull(manager.statusCheckerRegistry, "statusCheckerRegistry cannot be null!");
            Objects.requireNonNull(manager.observable, "observable cannot be null");
            Objects.requireNonNull(manager.telemetry, "telemetry cannot be null");
            Objects.requireNonNull(manager.policyArchive, "policyArchive cannot be null");
            Objects.requireNonNull(manager.transferProcessStore, "transferProcessStore cannot be null");
            Objects.requireNonNull(manager.addressResolver, "addressResolver cannot be null");
            Objects.requireNonNull(manager.provisionResponsesHandler, "provisionResultHandler cannot be null");
            Objects.requireNonNull(manager.deprovisionResponsesHandler, "deprovisionResponsesHandler cannot be null");

            manager.entityRetryProcessFactory = new EntityRetryProcessFactory(manager.monitor, manager.clock, manager.entityRetryProcessConfiguration);

            return manager;
        }
    }

}
