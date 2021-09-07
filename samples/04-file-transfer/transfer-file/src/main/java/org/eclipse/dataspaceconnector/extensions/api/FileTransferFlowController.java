package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

public class FileTransferFlowController implements DataFlowController {
    private final Monitor monitor;
    private final TypeManager typeManager;

    public FileTransferFlowController(Monitor monitor, TypeManager typeManager) {
        this.monitor = monitor;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return false;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        return null;
    }
}
