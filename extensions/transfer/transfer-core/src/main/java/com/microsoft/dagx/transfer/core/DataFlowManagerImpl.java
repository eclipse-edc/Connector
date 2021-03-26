package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.dagx.spi.transfer.response.ResponseStatus.FATAL_ERROR;

/**
 * The default data flow manager.
 */
public class DataFlowManagerImpl implements DataFlowManager {
    private List<DataFlowController> controllers = new ArrayList<>();

    @Override
    public void register(DataFlowController controller) {
        controllers.add(controller);
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiate(DataRequest dataRequest) {
        DataFlowController executor = getExecutor(dataRequest);
        if (executor == null) {
            return new DataFlowInitiateResponse(FATAL_ERROR,"Unable to process data request: " + dataRequest.getId());
        }
        return executor.initiateFlow(dataRequest);
    }

    @Nullable
    private DataFlowController getExecutor(DataRequest dataRequest) {
        for (DataFlowController manager : controllers) {
            if (manager.canHandle(dataRequest)) {
                return manager;
            }
        }
        return null;
    }
}
