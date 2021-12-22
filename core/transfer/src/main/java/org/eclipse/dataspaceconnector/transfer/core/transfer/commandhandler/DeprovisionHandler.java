package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.AsyncTransferProcessManager;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Deprovision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.End;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Transitions all processes that are in state DEPROVISIONING_REQ and deprovisions their associated
 * resources. Then they are moved to DEPROVISIONING
 *
 * @return the number of transfer processes in DEPROVISIONING_REQ
 */
public class DeprovisionHandler implements TransferProcessCommandHandler<Deprovision> {
    private final TransferProcessStore transferProcessStore;
    private final ProvisionManager provisionManager;
    private final Monitor monitor;
    private final Queue<AsyncTransferProcessManager.CommandRequest> commandQueue;

    public DeprovisionHandler(TransferProcessStore transferProcessStore, ProvisionManager provisionManager, Monitor monitor, Queue<AsyncTransferProcessManager.CommandRequest> commandQueue) {
        this.transferProcessStore = transferProcessStore;
        this.provisionManager = provisionManager;
        this.monitor = monitor;
        this.commandQueue = commandQueue;
    }

    @Override
    public Class<Deprovision> handles() {
        return Deprovision.class;
    }

    @Override
    public TransferProcessCommandResult handle(Deprovision command) {
        TransferProcess process = transferProcessStore.find(command.getId());

        var futures = provisionManager.deprovision(process);

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .whenComplete((empty, throwable) -> {
                        if (throwable == null && futures.stream().map(CompletableFuture::join).allMatch(response -> response.getStatus() == ResponseStatus.OK)) {
                            monitor.info("Deprovisioning successfully completed.");
                            process.transitionDeprovisioned();
                            commandQueue.add(new AsyncTransferProcessManager.CommandRequest(new End(process.getId()), new CompletableFuture<>()));
                        } else {
                            process.transitionError("Error during deprovisioning");
                        }
                        transferProcessStore.update(process);
                    });
        } catch (Exception e) {
            process.transitionError("Error during deprovisioning: " + e.getCause().getLocalizedMessage());
            transferProcessStore.update(process);
        }


        return new TransferProcessCommandResult(null, listener -> listener::deprovisioned);
    }

}
