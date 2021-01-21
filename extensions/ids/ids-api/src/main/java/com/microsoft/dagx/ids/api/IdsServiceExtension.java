package com.microsoft.dagx.ids.api;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import de.fraunhofer.iais.eis.DescriptionRequestMessageImpl;
import de.fraunhofer.iais.eis.DescriptionResponseMessageImpl;

/**
 * Implements the IDS Controller REST API.
 */
public class IdsServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context);

        // CatalogService catalogService = context.loadSingletonExtension(CatalogService.class);

        registerControllers(context);

        monitor.info("Initialized IDS API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS API extension");
    }

    private void registerTypes(ServiceExtensionContext context) {
        context.getTypeManager().registerTypes(DescriptionRequestMessageImpl.class);
        context.getTypeManager().registerTypes(DescriptionResponseMessageImpl.class);
    }

    private void registerControllers(ServiceExtensionContext context) {
        WebService webService = context.getService(WebService.class);
        webService.registerController(new DescriptionRequestController());
    }


}
