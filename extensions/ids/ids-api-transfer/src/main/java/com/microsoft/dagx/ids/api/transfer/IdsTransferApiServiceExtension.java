package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;

import java.util.Set;

import static com.microsoft.dagx.ids.spi.IdsConfiguration.CONNECTOR_NAME;

/**
 * Implements the IDS Controller REST API for data transfer services.
 */
public class IdsTransferApiServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("iam", "ids.core");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerControllers(context);

        monitor.info("Initialized IDS Transfer API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Transfer API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Transfer API extension");
    }

    private void registerControllers(ServiceExtensionContext context) {

        var connectorName = context.getSetting(CONNECTOR_NAME, "connectorName");

        WebService webService = context.getService(WebService.class);

        IdentityService identityService = context.getService(IdentityService.class);

        TransferManagerRegistry transferManagerRegistry = context.getService(TransferManagerRegistry.class);

        MetadataStore metadataStore = context.getService(MetadataStore.class);

        Monitor monitor = context.getMonitor();

        webService.registerController(new ArtifactRequestController(connectorName, identityService, metadataStore, transferManagerRegistry, monitor));
    }


}
