package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Initiate;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;

/**
 * Performs consumer-side or provider side provisioning for a service.
 * <br/>
 * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
 * map involve preprocessing data or other operations.
 */
public class InitiateHandler implements TransferProcessCommandHandler<Initiate> {

    private final TransferProcessStore transferProcessStore;

    public InitiateHandler(TransferProcessStore transferProcessStore) {
        this.transferProcessStore = transferProcessStore;
    }

    @Override
    public Class<Initiate> handles() {
        return Initiate.class;
    }

    @Override
    public TransferProcessCommandResult handle(Initiate command) {
        var dataRequest = command.getDataRequest();
        var processId = transferProcessStore.processIdForTransferId(dataRequest.getId());
        var id = command.getId();
        if (processId == null) {
            var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).type(command.getType()).build();
            process.transitionInitial();
            transferProcessStore.create(process);
        }
        return new TransferProcessCommandResult(new Provision(id), listener -> listener::created);
    }

}
