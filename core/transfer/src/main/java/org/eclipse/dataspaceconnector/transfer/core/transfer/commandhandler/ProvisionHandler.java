package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.AsyncTransferProcessManager;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlow;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireTransition;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;

/**
 * Performs consumer-side or provider side provisioning for a service.
 * <br/>
 * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
 * map involve preprocessing data or other operations.
 */
public class ProvisionHandler implements TransferProcessCommandHandler<Provision> {
    private final TransferProcessStore transferProcessStore;
    private final ProvisionManager provisionManager;
    private final Monitor monitor;
    private final Vault vault;
    private final TypeManager typeManager;
    private final Queue<AsyncTransferProcessManager.CommandRequest> commandQueue;

    public ProvisionHandler(TransferProcessStore transferProcessStore, ProvisionManager provisionManager, Monitor monitor, Vault vault, TypeManager typeManager, Queue<AsyncTransferProcessManager.CommandRequest> commandQueue) {
        this.transferProcessStore = transferProcessStore;
        this.provisionManager = provisionManager;
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
        this.commandQueue = commandQueue;
    }

    @Override
    public Class<Provision> handles() {
        return Provision.class;
    }

    @Override
    public TransferProcessCommandResult handle(Provision command) {
        TransferProcess process = transferProcessStore.find(command.getId());
        TransferProcessCommand nextCommand;
        if (process.getResourceManifest().getDefinitions().isEmpty()) {
            // TODO will not needed since provision manager will return the future
            nextCommand = process.getType() == CONSUMER ? new RequireTransition(process.getId()) : new InitiateDataFlow(process.getId());
            process.transitionProvisioned();
        } else {
            nextCommand = null;
            provisionManager.provision(process)
                    .forEach(future -> future.whenComplete((response, throwable) -> {
                        if (response != null) {
                            onProvisionComplete(response.getResource(), response.getSecretToken());
                        } else {
                            monitor.severe("Error provisioning resource", throwable);
                        }
                    }));

        }

        transferProcessStore.update(process);

        return new TransferProcessCommandResult(nextCommand, listener -> listener::provisioned);
    }


    void onProvisionComplete(ProvisionedDataDestinationResource destinationResource, SecretToken secretToken) {
        var processId = destinationResource.getTransferProcessId();
        var transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe(format("Error received when provisioning resource %s Process id not found for: %s",
                    destinationResource.getResourceDefinitionId(), destinationResource.getTransferProcessId()));
            return;
        }

        if (!destinationResource.isError()) {
            transferProcess.getDataRequest().updateDestination(destinationResource.createDataDestination());
        }

        if (secretToken != null) {
            String keyName = destinationResource.getResourceName();
            vault.storeSecret(keyName, typeManager.writeValueAsString(secretToken));
            transferProcess.getDataRequest().getDataDestination().setKeyName(keyName);

        }

        transferProcess.addProvisionedResource(destinationResource);

        if (destinationResource.isError()) {
            var processId1 = transferProcess.getId();
            var resourceId = destinationResource.getResourceDefinitionId();
            monitor.severe(format("Error provisioning resource %s for process %s: %s", resourceId, processId1, destinationResource.getErrorMessage()));
            transferProcessStore.update(transferProcess);
            return;
        }

        if (TransferProcessStates.ERROR.code() != transferProcess.getState() && transferProcess.provisioningComplete()) {
            // TODO If all resources provisioned, delete scratch data
            transferProcess.transitionProvisioned();
            // provision complete, we can go on with transition request or dataflow initialization
            var command = transferProcess.getType() == CONSUMER ? new RequireTransition(transferProcess.getId()) : new InitiateDataFlow(transferProcess.getId());
            commandQueue.add(new AsyncTransferProcessManager.CommandRequest(command, new CompletableFuture<>()));
        }
        transferProcessStore.update(transferProcess);
    }


}
