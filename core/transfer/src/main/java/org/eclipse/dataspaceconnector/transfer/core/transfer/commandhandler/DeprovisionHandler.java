package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Deprovision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.End;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

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

    public DeprovisionHandler(TransferProcessStore transferProcessStore, ProvisionManager provisionManager, Monitor monitor) {
        this.transferProcessStore = transferProcessStore;
        this.provisionManager = provisionManager;
        this.monitor = monitor;
    }

    @Override
    public Class<Deprovision> handles() {
        return Deprovision.class;
    }

    @Override
    public TransferProcessCommandResult handle(Deprovision command) {
        TransferProcess process = transferProcessStore.find(command.getId());
        TransferProcessCommand nextCommand;

        var futures = provisionManager.deprovision(process);

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            Stream<DeprovisionResponse> responses = futures.stream().map(CompletableFuture::join);
            if (responses.anyMatch(response -> response.getStatus() != ResponseStatus.OK)) {
                process.transitionError("Error during deprovisioning");
                nextCommand = null;
            } else {
                monitor.info("Deprovisioning successfully completed.");
                process.transitionDeprovisioned();
                nextCommand = new End(process.getId());
            }
        } catch (Exception e) {
            process.transitionError("Error during deprovisioning: " + e.getCause().getLocalizedMessage());
            nextCommand = null;
        }

        transferProcessStore.update(process);

        return new TransferProcessCommandResult(nextCommand, listener -> listener::deprovisioned);
    }

}
