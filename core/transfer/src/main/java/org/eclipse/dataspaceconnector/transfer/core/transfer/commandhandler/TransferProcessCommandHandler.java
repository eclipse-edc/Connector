package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

public interface TransferProcessCommandHandler<C extends TransferProcessCommand> {

    Class<C> handles();

    TransferProcessCommandResult handle(C command);

}
