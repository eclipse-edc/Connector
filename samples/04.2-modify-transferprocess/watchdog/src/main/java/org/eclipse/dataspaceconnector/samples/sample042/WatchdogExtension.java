package org.eclipse.dataspaceconnector.samples.sample042;

import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommandHandlerRegistry;

public class WatchdogExtension implements ServiceExtension {

    @Inject
    private TransferProcessManager manager;

    @Inject
    private TransferProcessStore store;

    @Inject
    private TransferProcessCommandHandlerRegistry commandHandlerRegistry;
    private Watchdog wd;

    @Override
    public void initialize(ServiceExtensionContext context) {
        commandHandlerRegistry.register(new CheckTimeoutCommandHandler(store, context.getMonitor()));
        wd = new Watchdog(manager, context.getMonitor());
    }

    @Override
    public void start() {
        wd.start();
    }

    @Override
    public void shutdown() {
        wd.stop();
    }
}
