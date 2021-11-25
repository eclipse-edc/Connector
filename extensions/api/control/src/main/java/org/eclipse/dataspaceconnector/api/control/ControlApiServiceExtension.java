package org.eclipse.dataspaceconnector.api.control;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.Set;

public class ControlApiServiceExtension implements ServiceExtension {

    private static final String NAME = "EDC Control API extension";

    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("edc:webservice", "dataspaceconnector:transfer-process-manager", "dataspaceconnector:dispatcher");
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        WebService webService = serviceExtensionContext.getService(WebService.class);
        TransferProcessManager transferProcessManager = serviceExtensionContext.getService(TransferProcessManager.class);
        RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry = serviceExtensionContext.getService(RemoteMessageDispatcherRegistry.class);

        webService.registerController(new ClientController(transferProcessManager));
        webService.registerController(new ClientControlCatalogApiController(remoteMessageDispatcherRegistry));

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }
}
