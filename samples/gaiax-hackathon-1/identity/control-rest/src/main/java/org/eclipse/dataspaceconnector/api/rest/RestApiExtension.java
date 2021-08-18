package org.eclipse.dataspaceconnector.api.rest;

import org.eclipse.dataspaceconnector.metadata.catalog.CatalogService;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.Set;
import java.util.UUID;


public class RestApiExtension implements ServiceExtension {


    @Override
    public Set<String> requires() {
        return Set.of("dataspaceconnector:transferprocessstore", "edc:catalog-service");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webService = context.getService(WebService.class);
        var monitor = context.getMonitor();

        // get all required services
        var transferProcessManager = context.getService(TransferProcessManager.class);
        var processStore = context.getService(TransferProcessStore.class);
        var catalogService = context.getService(CatalogService.class);

        ApiController controller = new ApiController(getConnectorId(context), monitor, transferProcessManager, processStore, catalogService);
        webService.registerController(controller);

        monitor.info("Initialized RestApiExtension");
    }

    private String getConnectorId(ServiceExtensionContext context) {
        return context.getSetting("edc.ids.connector.name", "edc-connector-" + UUID.randomUUID());
    }
}
