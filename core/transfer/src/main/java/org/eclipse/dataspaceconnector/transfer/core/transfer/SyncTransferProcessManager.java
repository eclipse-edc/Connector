package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
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

/**
 * Instead of inserting a {@link TransferProcess} into - and having it traverse through - the state machine implemented by the
 * {@link AsyncTransferProcessManager}, this implementation returns immediately (= "synchronously"). The {@link TransferProcess} is created
 * in the {@link TransferProcessStates#COMPLETED} state.
 * <p>
 * On the provider side the {@link DataProxyManager} checks if a {@link org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxy} is registered for a particular request and if so, calls it.
 * <p>
 * On the consumer side there is a set of {@link ProxyEntryHandler} instances, that receive the resulting {@link ProxyEntry} object. If a {@link ProxyEntryHandler} is registered, the {@link ProxyEntry}
 * is forwarded to it, and if no {@link ProxyEntryHandler} is registered, the {@link ProxyEntry} object is returned.
 */
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
    public TransferInitiateResult initiateConsumerRequest(DataRequest dataRequest) {
        var id = UUID.randomUUID().toString();
        var transferProcess = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).state(TransferProcessStates.COMPLETED.code()).type(CONSUMER).build();
        if (transferProcess.getState() == TransferProcessStates.UNSAVED.code()) {
            transferProcess.transitionInitial();
        }
        transferProcessStore.create(transferProcess);

        var future = dispatcherRegistry.send(Object.class, dataRequest, transferProcess::getId);
        try {
            var result = future.join();
            // reload from store, could have been modified
            transferProcess = transferProcessStore.find(transferProcess.getId());

            if (transferProcess.getState() == TransferProcessStates.ERROR.code()) {
                return TransferInitiateResult.error(dataRequest.getId(), ResponseStatus.FATAL_ERROR, transferProcess.getErrorDetail());
            }

            // we expect "result" to have a string field named "payload"
            // and we convert that into a ProxyEntry
            var proxyEntry = convert(result);

            // if there is one or more handlers for this particular transfer type, return the result of these handlers, otherwise return the
            // raw proxy object
            var handler = Optional.ofNullable(proxyEntryHandlers.get(proxyEntry.getType()));
            var proxyConversionResult = handler.map(peh -> peh.accept(dataRequest, proxyEntry)).orElse(proxyEntry);
            return TransferInitiateResult.success(dataRequest.getId(), proxyConversionResult);
        } catch (Exception ex) {
            var status = isRetryable(ex.getCause()) ? ResponseStatus.ERROR_RETRY : ResponseStatus.FATAL_ERROR;
            return TransferInitiateResult.error(dataRequest.getId(), status, ex.getMessage());
        }
    }

    @Override
    public TransferInitiateResult initiateProviderRequest(DataRequest dataRequest) {
        //create a transfer process in the COMPLETED state
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).state(TransferProcessStates.COMPLETED.code()).type(PROVIDER).build();
        if (process.getState() == TransferProcessStates.UNSAVED.code()) {
            process.transitionInitial();
        }
        transferProcessStore.create(process);

        var dataProxy = dataProxyManager.getProxy(dataRequest);
        if (dataProxy != null) {
            var proxyData = dataProxy.getData(dataRequest);
            return TransferInitiateResult.success(process.getId(), proxyData);
        }
        return TransferInitiateResult.error(process.getId(), ResponseStatus.FATAL_ERROR);
    }

    @Override
    public void enqueueCommand(Command command) {
        //noop
    }

    private ProxyEntry convert(Object result) {
        try {
            var payloadField = result.getClass().getDeclaredField("payload");
            payloadField.setAccessible(true);
            var payload = payloadField.get(result).toString();

            return typeManager.readValue(payload, ProxyEntry.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return ProxyEntry.Builder.newInstance().build();
        }
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof ConnectException) { //we might need to add more retryable exceptions
            return true;
        }
        return false;
    }

}
