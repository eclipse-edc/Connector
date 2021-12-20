package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Complete;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlow;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlowConsumer;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import static java.lang.String.format;

/**
 * Transition all processes, who have provisioned resources, into the IN_PROGRESS or STREAMING status, depending on
 * whether they're finite or not.
 * If a process does not have provisioned resources, it will remain in REQUESTED_ACK.
 */
public class InitiateDataFlowConsumerHandler implements TransferProcessCommandHandler<InitiateDataFlowConsumer> {

    private final TransferProcessStore transferProcessStore;
    private final Monitor monitor;

    public InitiateDataFlowConsumerHandler(TransferProcessStore transferProcessStore, Monitor monitor) {
        this.transferProcessStore = transferProcessStore;
        this.monitor = monitor;
    }

    @Override
    public Class<InitiateDataFlowConsumer> handles() {
        return InitiateDataFlowConsumer.class;
    }

    @Override
    public TransferProcessCommandResult handle(InitiateDataFlowConsumer command) {
        var process = transferProcessStore.find(command.getId());
        if (!process.getDataRequest().isManagedResources() || (process.getProvisionedResourceSet() != null && !process.getProvisionedResourceSet().empty())) {

            TransferProcessCommand nextCommand = null;
            if (process.getDataRequest().getTransferType().isFinite()) {
                process.transitionInProgress();
                nextCommand = new Complete(command.getId());
            } else {
                process.transitionStreaming();
                // nextCommand = TODO: streaming should be handled
            }
            transferProcessStore.update(process);
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            return new TransferProcessCommandResult(nextCommand, listener -> listener::inProgress);
        } else {
            monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED_ACK);
            return new TransferProcessCommandResult(null, listener -> null);
        }
    }

}
