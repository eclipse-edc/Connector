package org.eclipse.dataspaceconnector.extension.jetty;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebServer;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides({ WebServer.class, JettyService.class })
public class JettyExtension implements ServiceExtension {


    @EdcSetting
    private static final String KEYSTORE_PASSWORD = "keystore.password";
    @EdcSetting
    private static final String KEYMANAGER_PASSWORD = "keymanager.password";

    private JettyService jettyService;

    @Override
    public String name() {
        return "Jetty Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var configuration = JettyConfiguration.createFromConfig(context.getSetting(KEYSTORE_PASSWORD, "password"),
                context.getSetting(KEYMANAGER_PASSWORD, "password"), context.getConfig());

        jettyService = new JettyService(configuration, monitor);
        context.registerService(JettyService.class, jettyService);
        context.registerService(WebServer.class, jettyService);
    }

    @Override
    public void start() {
        jettyService.start();
    }

    @Override
    public void shutdown() {
        if (jettyService != null) {
            jettyService.shutdown();
        }
    }

}
