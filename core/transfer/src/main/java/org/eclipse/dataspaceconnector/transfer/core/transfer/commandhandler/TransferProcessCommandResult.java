package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import java.util.function.Consumer;
import java.util.function.Function;

public class TransferProcessCommandResult {
    private final TransferProcessCommand nextCommand;
    private final Function<TransferProcessListener, Consumer<TransferProcess>> postAction;

    public TransferProcessCommandResult(TransferProcessCommand nextCommand, Function<TransferProcessListener, Consumer<TransferProcess>> postAction) {
        this.nextCommand = nextCommand;
        this.postAction = postAction;
    }

    public TransferProcessCommand getNextCommand() {
        return this.nextCommand;
    }

    public Function<TransferProcessListener, Consumer<TransferProcess>> getPostAction() {
        return postAction;
    }
}
