package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Complete;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlow;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import static java.lang.String.format;

/**
 * On a consumer, sends provisioned requests to the provider connector. On the provider, sends provisioned requests to the data flow manager.
 *
 * @return the number of requests processed
 */
public class InitiateDataFlowHandler implements TransferProcessCommandHandler<InitiateDataFlow> {

    private final TransferProcessStore transferProcessStore;
    private final DataFlowManager dataFlowManager;
    private final Monitor monitor;

    public InitiateDataFlowHandler(TransferProcessStore transferProcessStore, DataFlowManager dataFlowManager, Monitor monitor) {
        this.transferProcessStore = transferProcessStore;
        this.dataFlowManager = dataFlowManager;
        this.monitor = monitor;
    }

    @Override
    public Class<InitiateDataFlow> handles() {
        return InitiateDataFlow.class;
    }

    @Override
    public TransferProcessCommandResult handle(InitiateDataFlow command) {
        var process = transferProcessStore.find(command.getId());
        var response = dataFlowManager.initiate(process.getDataRequest());
        if (response.failed()) {
            ResponseStatus failureStatus = response.getFailure().status();
            if (ResponseStatus.ERROR_RETRY == failureStatus) {
                monitor.severe("Error processing transfer request. Setting to retry: " + process.getId());
                process.transitionProvisioned();
                transferProcessStore.update(process);
                return new TransferProcessCommandResult(command, listener -> listener::provisioned);
            } else {
                String errorMessage = String.join(", ", response.getFailureMessages());
                monitor.severe(format("Fatal error processing transfer request: %s. Error details: %s", process.getId(), errorMessage));
                process.transitionError(errorMessage);
                transferProcessStore.update(process);
                return new TransferProcessCommandResult(null, listener -> listener::error);
            }
        } else {
            TransferProcessCommand nextCommand = null;
            if (process.getDataRequest().getTransferType().isFinite()) {
                process.transitionInProgress();
                nextCommand = new Complete(command.getId());
            } else {
                process.transitionStreaming();
                // nextCommand = TODO: streaming should be handled
            }
            transferProcessStore.update(process);

            return new TransferProcessCommandResult(nextCommand, listener -> listener::inProgress);
        }
    }

}
