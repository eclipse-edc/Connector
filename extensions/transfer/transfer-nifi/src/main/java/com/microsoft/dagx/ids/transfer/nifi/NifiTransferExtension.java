package com.microsoft.dagx.ids.transfer.nifi;

import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;

public class NifiTransferExtension implements ServiceExtension {
    @DagxSetting
    private static final String URL_SETTING = "nifi.url";

    private static final String DEFAULT_NIFI_URL = "http://localhost:8080/nifi-api";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerManager(context);

        monitor.info("Initialized Core Transfer extension");
    }

    @Override
    public void start() {
        monitor.info("Started Nifi Transfer extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Nifi Transfer extension");
    }

    private void registerManager(ServiceExtensionContext context) {
        TransferManagerRegistry transferManagerRegistry = context.getService(TransferManagerRegistry.class);

        String url = context.getSetting(URL_SETTING, DEFAULT_NIFI_URL);

        NifiTransferManagerConfiguration configuration = NifiTransferManagerConfiguration.Builder.newInstance().url(url).build();
        NifiTransferManager manager = new NifiTransferManager(configuration, context.getTypeManager(), context.getMonitor());
        transferManagerRegistry.register(manager);
    }

}
