package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.catalog.CatalogService;
import com.microsoft.dagx.ids.spi.catalog.CatalogServiceExtension;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import de.fraunhofer.iais.eis.DescriptionRequestMessageImpl;
import de.fraunhofer.iais.eis.DescriptionResponseMessageImpl;

/**
 * Implements the IDS Controller REST API.
 */
public class IdsCatalogApiExtension implements ServiceExtension {
    private Monitor monitor;
    private CatalogServiceExtension catalogServiceExtension;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context);

        // load the catalog extension
        catalogServiceExtension = context.loadSingletonExtension(CatalogServiceExtension.class);
        catalogServiceExtension.start();

        registerControllers(context);

        monitor.info("Initialized IDS Catalog API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Catalog API extension");
    }

    @Override
    public void shutdown() {
        catalogServiceExtension.stop();
        monitor.info("Shutdown IDS Catalog API extension");
    }

    private void registerTypes(ServiceExtensionContext context) {
        context.getTypeManager().registerTypes(DescriptionRequestMessageImpl.class);
        context.getTypeManager().registerTypes(DescriptionResponseMessageImpl.class);
    }

    private void registerControllers(ServiceExtensionContext context) {
        WebService webService = context.getService(WebService.class);
        CatalogService catalogService = catalogServiceExtension.getCatalogService();

        webService.registerController(new DescriptionRequestController(catalogService));
    }


}
