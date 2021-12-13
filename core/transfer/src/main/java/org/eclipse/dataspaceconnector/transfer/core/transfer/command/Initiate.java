package org.eclipse.dataspaceconnector.transfer.core.transfer.command;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

public class Initiate extends TransferProcessCommand {
    private final TransferProcess.Type type;
    private final DataRequest dataRequest;

    public Initiate(String id, TransferProcess.Type type, DataRequest dataRequest) {
        super(id);
        this.type = type;
        this.dataRequest = dataRequest;
    }

    public DataRequest getDataRequest() {
        return dataRequest;
    }

    public TransferProcess.Type getType() {
        return type;
    }
}
