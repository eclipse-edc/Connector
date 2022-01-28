package org.eclipse.dataspaceconnector.extensions.health;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Requires({WebService.class})
public class HealthEndpointExtension implements ServiceExtension {
    private static final String LOG_PREFIX_SETTING = "edc.samples.03.logprefix";

    @Override
    public void initialize(ServiceExtensionContext context) {
        var logPrefix = context.getSetting(LOG_PREFIX_SETTING, "health");
        var webService = context.getService(WebService.class);
        webService.registerController(new HealthApiController(context.getMonitor(), logPrefix));
    }
}
