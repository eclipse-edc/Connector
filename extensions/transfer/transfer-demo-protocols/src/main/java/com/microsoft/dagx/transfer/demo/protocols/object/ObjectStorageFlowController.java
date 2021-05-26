package com.microsoft.dagx.transfer.demo.protocols.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ObjectStorageFlowController implements DataFlowController {
    private ObjectMapper objectMapper;
    private Monitor monitor;

    public ObjectStorageFlowController(ObjectMapper objectMapper, Monitor monitor) {
        this.objectMapper = objectMapper;
        this.monitor = monitor;
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
