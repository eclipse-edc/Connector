package com.microsoft.dagx.loopback;

import com.microsoft.dagx.spi.message.MessageContext;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcher;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.message.RemoteMessage;
import com.microsoft.dagx.spi.types.domain.metadata.QueryRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;

import java.util.concurrent.CompletableFuture;

/**
 * Performs a loopback to the local runtime.
 */
public class LoopbackDispatcher implements RemoteMessageDispatcher {
    private TransferProcessManager processManager;
    private Vault vault;
    private Monitor monitor;

    public LoopbackDispatcher(TransferProcessManager processManager, Vault vault, Monitor monitor) {
        this.processManager = processManager;
        this.vault = vault;
        this.monitor = monitor;
    }

    @Override
    public String protocol() {
        return "loopback";
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
        var future = new CompletableFuture<>();
        if (message instanceof QueryRequest) {
            future.completeExceptionally(new UnsupportedOperationException("Not yet implemented"));
        } else if (message instanceof DataRequest) {
            future.complete(null);

            var originalRequest = (DataRequest) message;

            // create a different id since the runtime will have a client process registered with the same id
            var requestCopy = originalRequest.copy(originalRequest.getId() + "-provider");

            // copy the access token to the vault location the provider expects
            var keyName = requestCopy.getDataDestination().getKeyName();
            var token = vault.resolveSecret(DestinationSecretToken.KEY + "-" + originalRequest.getProcessId());
            vault.storeSecret(keyName, token);

            monitor.info("Received loopback data request: " + requestCopy.getId());

            processManager.initiateProviderRequest(requestCopy);
        }
        return (CompletableFuture<T>) future;
    }
}
