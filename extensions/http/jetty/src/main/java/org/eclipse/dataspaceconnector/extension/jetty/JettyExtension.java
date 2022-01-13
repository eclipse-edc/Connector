package org.eclipse.dataspaceconnector.extension.jetty;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static java.lang.String.format;

@Provides({ JettyService.class })
public class JettyExtension implements ServiceExtension {

    @EdcSetting
    private static final String HTTP_PORT = "web.http.port";
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

        var configuration = new JettyConfiguration(
                getPort(context),
                context.getSetting(KEYSTORE_PASSWORD, "password"),
                context.getSetting(KEYMANAGER_PASSWORD, "password")
        );

        jettyService = new JettyService(configuration, monitor);
        context.registerService(JettyService.class, jettyService);
    }

    private int getPort(ServiceExtensionContext context) {
        String portSetting = context.getSetting(HTTP_PORT, "8181");
        try {
            return Integer.parseInt(portSetting);
        } catch (NumberFormatException e) {
            throw new EdcException(format("Port %s is not a valid number", portSetting));
        }
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
