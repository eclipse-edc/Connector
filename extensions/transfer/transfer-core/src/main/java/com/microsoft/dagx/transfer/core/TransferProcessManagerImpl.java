package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferInitiateResponse;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.TransferWaitStrategy;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;

/**
 *
 */
public class TransferProcessManagerImpl implements TransferProcessManager {
    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds

    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private Monitor monitor;

    private ExecutorService executor;

    private AtomicBoolean active = new AtomicBoolean();

    public void start(TransferProcessStore processStore) {
        this.transferProcessStore = processStore;
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
    public TransferInitiateResponse initiate(DataRequest dataRequest) {
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).dataRequest(dataRequest).build();
        transferProcessStore.create(process);
        return TransferInitiateResponse.Builder.newInstance().id(process.getId()).status(ResponseStatus.OK).build();
    }

    private void run() {
        try {
            while (active.get()) {
                int provisioned = provisionInitial();

                // TODO check processes in provisioning state and timestamps for failed processes

                int sent = sendRequests();

                if (provisioned == 0 && sent == 0) {
                    //noinspection BusyWait
                    Thread.sleep(waitStrategy.waitForMillis());
                }
            }
        } catch (Error e) {
            throw e; // let the thread die and don't reschedule as the error is unrecoverable
        } catch (Throwable e) {
            monitor.severe("Error caught in transfer process manager", e);
        }
    }

    private int provisionInitial() {
        List<TransferProcess> processes = transferProcessStore.nextForState(INITIAL.code(), batchSize);
        for (TransferProcess process : processes) {
            ResourceManifest manifest = manifestGenerator.generate(process.getDataRequest());
            process.transitionProvisioning(manifest);
            transferProcessStore.update(process);
            provisionManager.provision(process);
        }
        return processes.size();
    }

    private int sendRequests() {
        List<TransferProcess> processes = transferProcessStore.nextForState(PROVISIONED.code(), batchSize);
        for (TransferProcess process : processes) {
            // TODO add request sender
            transferProcessStore.update(process);
        }
        return processes.size();
    }

    private TransferProcessManagerImpl() {
    }

    public static class Builder {
        private TransferProcessManagerImpl manager;

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

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public TransferProcessManagerImpl build() {
            return manager;
        }

        private Builder() {
            manager = new TransferProcessManagerImpl();
        }
    }
}
