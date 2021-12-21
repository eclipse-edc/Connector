package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.End;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.PrepareProvision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;

/**
 * Ends a deprovisioned transfer process
 */
public class EndHandler implements TransferProcessCommandHandler<End> {
    private final TransferProcessStore transferProcessStore;

    public EndHandler(TransferProcessStore transferProcessStore) {
        this.transferProcessStore = transferProcessStore;
    }

    @Override
    public Class<End> handles() {
        return End.class;
    }

    @Override
    public TransferProcessCommandResult handle(End command) {
        TransferProcess process = transferProcessStore.find(command.getId());
        process.transitionEnded();
        transferProcessStore.update(process);

        return new TransferProcessCommandResult(null, listener -> listener::ended);
    }

}
