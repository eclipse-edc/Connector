package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Complete;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

public class CompleteHandler implements TransferProcessCommandHandler<Complete> {
    private final TransferProcessStore transferProcessStore;
    private final StatusCheckerRegistry statusCheckerRegistry;
    private final Monitor monitor;

    public CompleteHandler(TransferProcessStore transferProcessStore, StatusCheckerRegistry statusCheckerRegistry, Monitor monitor) {
        this.transferProcessStore = transferProcessStore;
        this.statusCheckerRegistry = statusCheckerRegistry;
        this.monitor = monitor;
    }

    @Override
    public Class<Complete> handles() {
        return Complete.class;
    }

    @Override
    public TransferProcessCommandResult handle(Complete command) {
        var process = transferProcessStore.find(command.getId());
        if (process.getDataRequest().isManagedResources()) {
            var resources = process.getProvisionedResourceSet().getResources();
            var checker = statusCheckerRegistry.resolve(process.getDataRequest().getDestinationType());
            if (checker == null) {
                monitor.info(format("No checker found for process %s. The process will not advance to the COMPLETED state.", process.getId()));
                return new TransferProcessCommandResult(null, l -> null);
            } else if (checker.isComplete(process, resources)) {
                // checker passed, transition the process to the COMPLETED state
                process.transitionCompleted();
                monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.COMPLETED);
                transferProcessStore.update(process);
                return new TransferProcessCommandResult(null, listener -> listener::completed);
            }
        } else {
            var checker = statusCheckerRegistry.resolve(process.getDataRequest().getDestinationType());
            if (checker != null) {
                if (checker.isComplete(process, emptyList())) {
                    //checker passed, transition the process to the COMPLETED state automatically
                    process.transitionCompleted();
                    monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.COMPLETED);
                    transferProcessStore.update(process);
                    return new TransferProcessCommandResult(null, listener -> listener::completed);
                } else {
                    return new TransferProcessCommandResult(null, l -> null);
                }
            } else {
                //no checker, transition the process to the COMPLETED state automatically
                process.transitionCompleted();
                monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.COMPLETED);
                transferProcessStore.update(process);
                return new TransferProcessCommandResult(null, listener -> listener::completed);
            }
        }
        return null; // TODO: why is this needed?
    }

}
