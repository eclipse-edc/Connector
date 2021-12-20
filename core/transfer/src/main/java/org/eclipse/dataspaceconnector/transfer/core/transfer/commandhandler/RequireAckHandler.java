package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlowConsumer;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireAck;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.TransferProcessCommandHandler;

public class RequireAckHandler implements TransferProcessCommandHandler<RequireAck> {
    private final TransferProcessStore transferProcessStore;

    public RequireAckHandler(TransferProcessStore transferProcessStore) {
        this.transferProcessStore = transferProcessStore;
    }

    @Override
    public Class<RequireAck> handles() {
        return RequireAck.class;
    }

    @Override
    public TransferProcessCommandResult handle(RequireAck command) {
        TransferProcess transferProcess = transferProcessStore.find(command.getId());
        transferProcess.transitionRequestAck();
        transferProcessStore.update(transferProcess);
        return new TransferProcessCommandResult(new InitiateDataFlowConsumer(command.getId()), listener -> listener::inProgress);
    }
}
