package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlow;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireTransition;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

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

    public ProvisionHandler(TransferProcessStore transferProcessStore, ProvisionManager provisionManager) {
        this.transferProcessStore = transferProcessStore;
        this.provisionManager = provisionManager;
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
            provisionManager.provision(process); // TODO: on provision complete, do state change (provisioned)
        }

        transferProcessStore.update(process);

        return new TransferProcessCommandResult(nextCommand, listener -> listener::provisioned);
    }

}
