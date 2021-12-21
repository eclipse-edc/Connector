package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Deprovision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Performs consumer-side or provider side provisioning for a service.
 * <br/>
 * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
 * map involve preprocessing data or other operations.
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

        var responses = provisionManager.deprovision(process).stream()
                .map(future -> future.whenComplete((response, throwable) -> {
                    if (response != null) {
                        onDeprovisionComplete(response.getResource());
                    } else {
                        monitor.severe("Deprovisioning error: ", throwable);
                    }
                }))
                .map(CompletableFuture::join)
                .collect(toList());

        if (responses.stream().anyMatch(response -> response.getStatus() != ResponseStatus.OK)) {
            process.transitionError("Error during deprovisioning");
            transferProcessStore.update(process);
            nextCommand = null;
        } else {
            nextCommand = null; // should end transfer
        }

        return new TransferProcessCommandResult(nextCommand, listener -> listener::provisioned);
    }

    // TODO: should set deprovision complete when every destination is deprovisioned
    void onDeprovisionComplete(ProvisionedDataDestinationResource resource) {
        monitor.info("Deprovisioning successfully completed.");

        TransferProcess transferProcess = transferProcessStore.find(resource.getTransferProcessId());
        if (transferProcess != null) {
            transferProcess.transitionDeprovisioned();
            transferProcessStore.update(transferProcess);
            monitor.debug("Process " + transferProcess.getId() + " is now " + TransferProcessStates.from(transferProcess.getState()));
        } else {
            monitor.severe("ProvisionManager: no TransferProcess found for deprovisioned resource");
        }
    }

}
