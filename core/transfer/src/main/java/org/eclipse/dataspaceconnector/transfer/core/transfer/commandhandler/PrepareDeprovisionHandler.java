package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Deprovision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.PrepareDeprovision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;

/**
 * Performs consumer-side or provider side provisioning for a service.
 * <br/>
 * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
 * map involve preprocessing data or other operations.
 */
public class PrepareDeprovisionHandler implements TransferProcessCommandHandler<PrepareDeprovision> {
    private final TransferProcessStore transferProcessStore;

    public PrepareDeprovisionHandler(TransferProcessStore transferProcessStore) {
        this.transferProcessStore = transferProcessStore;
    }

    @Override
    public Class<PrepareDeprovision> handles() {
        return PrepareDeprovision.class;
    }

    @Override
    public TransferProcessCommandResult handle(PrepareDeprovision command) {
        var process = transferProcessStore.find(command.getId());
        process.transitionDeprovisioning();
        transferProcessStore.update(process);
        return new TransferProcessCommandResult(new Deprovision(process.getId()), listener -> listener::deprovisioning);
    }

}
