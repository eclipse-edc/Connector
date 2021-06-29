/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.core.transfer;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.*;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcess.Type.CLIENT;
import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates.*;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;

/**
 *
 */
public class TransferProcessManagerImpl implements TransferProcessManager, TransferProcessObservable {
    private final AtomicBoolean active = new AtomicBoolean();
    private final Map<String, List<TransferProcessListener>> listenerMap;
    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private Monitor monitor;
    private ExecutorService executor;
    private StatusCheckerRegistry statusCheckerRegistry;

    private TransferProcessManagerImpl() {
        listenerMap = new HashMap<>();
    }

    public Map<String, List<TransferProcessListener>> getListeners() {
        return listenerMap;
    }

    public void start(TransferProcessStore processStore) {
        transferProcessStore = processStore;
        active.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::run);
    }

    public void stop() {
        active.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public TransferInitiateResponse initiateClientRequest(DataRequest dataRequest) {
        return initiateRequest(CLIENT, dataRequest);
    }

    @Override
    public TransferInitiateResponse initiateProviderRequest(DataRequest dataRequest) {
        return initiateRequest(PROVIDER, dataRequest);
    }

    @Override
    public void registerListener(String processId, TransferProcessListener listener) {
        if (listenerMap.containsKey(processId)) {
            final List<TransferProcessListener> list = listenerMap.get(processId);
            if (!list.contains(listener)) {
                list.add(listener);
            }
        } else {
            final ArrayList<TransferProcessListener> list = new ArrayList<>();
            list.add(listener);
            listenerMap.put(processId, list);
        }
    }

    @Override
    public void unregister(TransferProcessListener listener) {
        // unregister from all processes
        listenerMap.forEach((key, value) -> value.remove(listener));
        // clear the registration if no more listeners
        listenerMap.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    private TransferInitiateResponse initiateRequest(TransferProcess.Type type, DataRequest dataRequest) {
        // make the request idempotent: if the process exists, return
        var processId = transferProcessStore.processIdForTransferId(dataRequest.getId());
        if (processId != null) {
            return TransferInitiateResponse.Builder.newInstance().id(processId).status(ResponseStatus.OK).build();
        }
        String id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).type(type).build();
        transferProcessStore.create(process);
        return TransferInitiateResponse.Builder.newInstance().id(process.getId()).status(ResponseStatus.OK).build();
    }

    private void run() {
        while (active.get()) {
            try {
                int provisioning = provisionInitialProcesses();

                // TODO check processes in provisioning state and timestamps for failed processes

                int sent = sendOrProcessProvisionedRequests();

                int provisioned = checkProvisioned();

                int finished = checkCompleted();

                int deprovisioning = checkDeprovisioningRequested();

                int deprovisioned = checkDeprovisioned();

                if (provisioning + provisioned + sent + finished + deprovisioning + deprovisioned == 0) {
                    Thread.sleep(waitStrategy.waitForMillis());
                }
                waitStrategy.success();
            } catch (Error e) {
                throw e; // let the thread die and don't reschedule as the error is unrecoverable
            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Throwable e) {
                monitor.severe("Error caught in transfer process manager", e);
                try {
                    Thread.sleep(waitStrategy.retryInMillis());
                } catch (InterruptedException e2) {
                    Thread.interrupted();
                    active.set(false);
                    break;
                }
            }
        }
    }


    private int checkDeprovisioned() {
        var deprovisionedProcesses = transferProcessStore.nextForState(DEPROVISIONED.code(), batchSize);

        for (var process : deprovisionedProcesses) {
            publishDeprovisioned(process);
            process.transitionEnded();
            transferProcessStore.update(process);
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
        }
        return deprovisionedProcesses.size();
    }

    /**
     * Transitions all processes that are in state DEPROVISIONING_REQ and deprovisions their associated
     * resources. Then they are moved to DEPROVISIONING
     *
     * @return the number of transfer processes in DEPROVISIONING_REQ
     */
    private int checkDeprovisioningRequested() {
        List<TransferProcess> processesDeprovisioning = transferProcessStore.nextForState(DEPROVISIONING_REQ.code(), batchSize);

        for (var process : processesDeprovisioning) {
            process.transitionDeprovisioning();
            transferProcessStore.update(process);
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            provisionManager.deprovision(process);
        }

        return processesDeprovisioning.size();
    }


    /**
     * Transition all processes, who have provisioned resources, into the IN_PROCRESS or STREAMING status, depending on
     * whether they're finite or not.
     * If a process does not have provisioned resources, it will remain in REQUESTED_ACK.
     */
    private int checkProvisioned() {
        List<TransferProcess> requestAcked = transferProcessStore.nextForState(TransferProcessStates.REQUESTED_ACK.code(), batchSize);

        for (var process : requestAcked) {
            if (!process.getProvisionedResourceSet().empty()) {

                if (process.getDataRequest().getTransferType().isFinite()) {
                    process.transitionInProgress();
                } else {
                    process.transitionStreaming();
                }
                monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            } else {
                monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED_ACK);
            }
            transferProcessStore.update(process);
        }

        return requestAcked.size();

    }

    /**
     * Checks all provisioned resources that are assigned to a transfer process for completion. If no StatusChecker exists
     * for a particular ProvisionedResource, it is automatically assumed to be complete.
     */
    private int checkCompleted() {

        //deal with all the client processes
        List<TransferProcess> processesInProgress = transferProcessStore.nextForState(TransferProcessStates.IN_PROGRESS.code(), batchSize);

        for (var process : processesInProgress.stream().filter(p -> p.getType() == CLIENT).collect(Collectors.toList())) {

            List<ProvisionedResource> resources = process.getProvisionedResourceSet().getResources().stream().filter(this::hasChecker).collect(Collectors.toList());

            // update the process once ALL resources are completed
            if (resources.stream().allMatch(this::isComplete)) {
                process.transitionCompleted();
                monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.COMPLETED);
                publishCompleted(process);

            }
            transferProcessStore.update(process);
        }
        return processesInProgress.size();
    }


    /**
     * Performs client-side or provider side provisioning for a service.
     * <p>
     * On a client, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
     * map involve preprocessing data or other operations.
     */
    private int provisionInitialProcesses() {
        List<TransferProcess> processes = transferProcessStore.nextForState(INITIAL.code(), batchSize);
        for (TransferProcess process : processes) {
            DataRequest dataRequest = process.getDataRequest();
            ResourceManifest manifest;
            if (process.getType() == CLIENT) {
                // if resources are managed by this connector, generate the manifest; otherwise create an empty one
                manifest = dataRequest.isManagedResources() ? manifestGenerator.generateClientManifest(process) : ResourceManifest.Builder.newInstance().build();
            } else {
                manifest = manifestGenerator.generateProviderManifest(process);
            }
            process.transitionProvisioning(manifest);
            transferProcessStore.update(process);
            provisionManager.provision(process);
        }
        return processes.size();
    }

    /**
     * On a client, sends provisioned requests to the provider connector. On the provider, sends provisioned requests to the data flow manager.
     *
     * @return the number of requests processed
     */
    private int sendOrProcessProvisionedRequests() {
        List<TransferProcess> processes = transferProcessStore.nextForState(PROVISIONED.code(), batchSize);
        for (TransferProcess process : processes) {
            DataRequest dataRequest = process.getDataRequest();
            if (CLIENT == process.getType()) {
                process.transitionRequested();
                transferProcessStore.update(process);   // update before sending to accommodate synchronous transports; reliability will be managed by retry and idempotency
                dispatcherRegistry.send(Void.class, dataRequest, process::getId);
            } else {
                var response = dataFlowManager.initiate(dataRequest);
                if (ResponseStatus.ERROR_RETRY == response.getStatus()) {
                    monitor.severe("Error processing transfer request. Setting to retry: " + process.getId());
                    process.transitionProvisioned();
                } else if (ResponseStatus.FATAL_ERROR == response.getStatus()) {
                    monitor.severe(format("Fatal error processing transfer request: %s. Error details: %s", process.getId(), response.getError()));
                    process.transitionError(response.getError());
                } else {
                    if (process.getDataRequest().getTransferType().isFinite()) {
                        process.transitionInProgress();
                    } else {
                        process.transitionStreaming();
                    }
                }
            }
            transferProcessStore.update(process);
        }
        return processes.size();
    }

    private void publishCompleted(TransferProcess process) {
        final List<TransferProcessListener> transferProcessListeners = listenerMap.get(process.getId());
        if (transferProcessListeners != null) {
            transferProcessListeners.forEach(l -> l.completed(process));
        }
    }

    private void publishDeprovisioned(TransferProcess process) {
        final List<TransferProcessListener> transferProcessListeners = listenerMap.get(process.getId());
        if (transferProcessListeners != null) {
            transferProcessListeners.forEach(l -> l.deprovisioned(process));
        }
    }

    private boolean hasChecker(ProvisionedResource provisionedResource) {
        return provisionedResource instanceof ProvisionedDataDestinationResource && statusCheckerRegistry.resolve((ProvisionedDataDestinationResource) provisionedResource) != null;
    }

    private boolean isComplete(ProvisionedResource resource) {
        if (!(resource instanceof ProvisionedDataDestinationResource)) {
            return false;
        }
        ProvisionedDataDestinationResource dataResource = (ProvisionedDataDestinationResource) resource;
        var checker = statusCheckerRegistry.resolve(dataResource);
        if (checker == null) {
            return true;
        }
        return checker.isComplete(dataResource);
    }

    public static class Builder {
        private final TransferProcessManagerImpl manager;

        private Builder() {
            manager = new TransferProcessManagerImpl();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder batchSize(int size) {
            manager.batchSize = size;
            return this;
        }

        public Builder waitStrategy(TransferWaitStrategy waitStrategy) {
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

        public TransferProcessManagerImpl build() {
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator");
            Objects.requireNonNull(manager.provisionManager, "provisionManager");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.statusCheckerRegistry, "StatusCheckerRegistry cannot be null!");
            return manager;
        }
    }
}
