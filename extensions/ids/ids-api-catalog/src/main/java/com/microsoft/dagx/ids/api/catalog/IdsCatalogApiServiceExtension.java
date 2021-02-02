package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.descriptor.IdsDescriptorService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import de.fraunhofer.iais.eis.DescriptionRequestMessageImpl;
import de.fraunhofer.iais.eis.DescriptionResponseMessageImpl;

/**
 * Implements the IDS Controller REST API for catalog services.
 */
public class IdsCatalogApiServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context);

        // load the catalog extension

        registerControllers(context);

        monitor.info("Initialized IDS Catalog API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Catalog API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Catalog API extension");
    }

    private void registerTypes(ServiceExtensionContext context) {
        context.getTypeManager().registerTypes(DescriptionRequestMessageImpl.class);
        context.getTypeManager().registerTypes(DescriptionResponseMessageImpl.class);
    }

    private void registerControllers(ServiceExtensionContext context) {
        WebService webService = context.getService(WebService.class);
        IdsDescriptorService descriptorService = context.getService(IdsDescriptorService.class);
        MetadataStore metadataStore = context.getService(MetadataStore.class);

        webService.registerController(new DescriptionRequestController(descriptorService, metadataStore));
    }


}
