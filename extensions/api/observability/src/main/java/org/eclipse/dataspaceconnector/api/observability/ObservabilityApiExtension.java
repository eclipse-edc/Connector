package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

public class ObservabilityApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private HealthCheckService healthCheckService;

    @Override
    public String name() {
        return "Observability API";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {


        webService.registerResource(new ObservabilityApiController(healthCheckService));

        // contribute to the liveness probe
        healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
        healthCheckService.addLivenessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
    }

}
