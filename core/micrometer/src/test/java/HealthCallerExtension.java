import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * This extension is an extension used to test Micrometer metrics.
 * It provides an endpoint that will call the health endpoint. It calls the health endpoint to be able to test
 * the OkHttpMetrics.
 */
@Requires({WebService.class})
public class HealthCallerExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webService = context.getService(WebService.class);
        webService.registerResource(new HealthCallerController(context.getMonitor(), new OkHttpClient()));
    }
}
