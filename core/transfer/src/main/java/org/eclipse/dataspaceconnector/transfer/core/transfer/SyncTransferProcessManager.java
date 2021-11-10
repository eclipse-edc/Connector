package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.net.ConnectException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;

public class SyncTransferProcessManager implements TransferProcessManager {

    private final DataProxyManager dataProxyManager;
    private final TransferProcessStore transferProcessStore;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final Map<String, ProxyEntryHandler> proxyEntryHandlers;
    private final TypeManager typeManager;

    public SyncTransferProcessManager(DataProxyManager dataProxyManager, TransferProcessStore transferProcessStore, RemoteMessageDispatcherRegistry dispatcherRegistry, Map<String, ProxyEntryHandler> proxyEntryHandlers, TypeManager typeManager) {
        this.dataProxyManager = dataProxyManager;
        this.transferProcessStore = transferProcessStore;
        this.dispatcherRegistry = dispatcherRegistry;
        this.proxyEntryHandlers = proxyEntryHandlers;
        this.typeManager = typeManager;
    }

    @Override
    public TransferResponse initiateConsumerRequest(DataRequest dataRequest) {
        var id = UUID.randomUUID().toString();
        var transferProcess = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).state(TransferProcessStates.COMPLETED.code()).type(CONSUMER).build();
        transferProcessStore.create(transferProcess);

        var future = dispatcherRegistry.send(Object.class, dataRequest, transferProcess::getId);
        try {
            var result = future.join();
            // reload from store, could have been modified
            transferProcess = transferProcessStore.find(transferProcess.getId());

            if (transferProcess.getState() == TransferProcessStates.ERROR.code()) {
                return TransferResponse.Builder.newInstance().error(transferProcess.getErrorDetail()).id(dataRequest.getId()).status(ResponseStatus.FATAL_ERROR).build();
            }

            var proxyEntry = convert(result);

            // if there is one or more handlers for this particular transfer type, return the result of these handlers, otherwise return the
            // raw proxy object
            var handler = Optional.ofNullable(proxyEntryHandlers.get(proxyEntry.getType()));
            var proxyConversionResult = handler.map(peh -> peh.apply(proxyEntry)).orElse(proxyEntry);
            return TransferResponse.Builder.newInstance().data(proxyConversionResult).id(dataRequest.getId()).status(ResponseStatus.OK).build();
        } catch (Exception ex) {
            var status = isRetryable(ex.getCause()) ? ResponseStatus.ERROR_RETRY : ResponseStatus.FATAL_ERROR;
            return TransferResponse.Builder.newInstance().id(dataRequest.getId()).status(status).error(ex.getMessage()).build();
        }
    }

    @Override
    public TransferResponse initiateProviderRequest(DataRequest dataRequest) {
        //create a transfer process in the COMPLETED state
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).state(TransferProcessStates.COMPLETED.code()).type(PROVIDER).build();
        transferProcessStore.create(process);

        var dataProxy = dataProxyManager.getProxy(dataRequest);
        if (dataProxy != null) {
            var proxyData = dataProxy.getData(dataRequest);
            return TransferResponse.Builder.newInstance().id(process.getId()).data(proxyData).status(ResponseStatus.OK).build();
        }
        return TransferResponse.Builder.newInstance().id(process.getId()).status(ResponseStatus.FATAL_ERROR).build();
    }

    private ProxyEntry convert(Object result) {
        return typeManager.readValue(typeManager.writeValueAsString(result), ProxyEntry.class);
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof ConnectException) { //we might need to add more retryable exceptions
            return true;
        }
        return false;
    }

}
