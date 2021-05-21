package com.microsoft.dagx.loopback;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;

/**
 * Provides a {@link com.microsoft.dagx.spi.message.RemoteMessageDispatcher} that loops requests back to the local runtime. Intended for testing.
 */
public class LoopbackServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        var messageDispatcher = context.getService(RemoteMessageDispatcherRegistry.class);
        var processManager = context.getService(TransferProcessManager.class);

        messageDispatcher.register(new LoopbackDispatcher(processManager, monitor));

        monitor.info("Initialized Loopback extension");
    }

    @Override
    public void start() {
        monitor.info("Started Loopback extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Loopback extension");
    }

}
