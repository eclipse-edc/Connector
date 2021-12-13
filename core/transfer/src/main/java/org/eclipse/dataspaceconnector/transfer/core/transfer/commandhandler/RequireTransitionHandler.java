package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireTransition;

public class RequireTransitionHandler implements TransferProcessCommandHandler<RequireTransition>  {

    private final TransferProcessStore transferProcessStore;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public RequireTransitionHandler(TransferProcessStore transferProcessStore, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.transferProcessStore = transferProcessStore;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public Class<RequireTransition> handles() {
        return RequireTransition.class;
    }

    @Override
    public TransferProcessCommandResult handle(RequireTransition command) {
        TransferProcess process = transferProcessStore.find(command.getId());
        process.transitionRequested();
        transferProcessStore.update(process);   // update before sending to accommodate synchronous transports; reliability will be managed by retry and idempotency
        dispatcherRegistry.send(Void.class, process.getDataRequest(), process::getId);
        return new TransferProcessCommandResult(null, listener -> listener::requested);
    }

}
