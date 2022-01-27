package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.command.commands.DeprovisionRequest;

public class DeprovisionRequestHandler extends TransferProcessCommandHandler<DeprovisionRequest> {

    public DeprovisionRequestHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<DeprovisionRequest> getType() {
        return DeprovisionRequest.class;
    }

    @Override
    protected boolean modify(TransferProcess process) {
        process.transitionDeprovisionRequested();
        return true;
    }
}
