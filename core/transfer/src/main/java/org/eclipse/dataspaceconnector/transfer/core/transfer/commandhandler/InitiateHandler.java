package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Initiate;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;

import java.util.function.Consumer;

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
            transferProcessStore.create(process);
        }
        return new TransferProcessCommandResult(new Provision(id), listener -> listener::created);
    }

}
