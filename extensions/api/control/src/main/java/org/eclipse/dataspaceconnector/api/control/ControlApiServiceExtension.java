package org.eclipse.dataspaceconnector.api.control;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Set;
import java.util.function.Predicate;

public class ControlApiServiceExtension implements ServiceExtension {

    private static final String NAME = "EDC Control API extension";

    @EdcSetting
    public static final String EDC_API_CONTROL_AUTH_APIKEY_KEY = "edc.api.control.auth.apikey.key";
    public static final String EDC_API_CONTROL_AUTH_APIKEY_KEY_DEFAULT = "X-Api-Key";

    @EdcSetting
    public static final String EDC_API_CONTROL_AUTH_APIKEY_VALUE = "edc.api.control.auth.apikey.value";

    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("edc:webservice", "dataspaceconnector:transfer-process-manager", "dataspaceconnector:dispatcher");
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        WebService webService = serviceExtensionContext.getService(WebService.class);
        TransferProcessManager transferProcessManager = serviceExtensionContext.getService(TransferProcessManager.class);
        RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry = serviceExtensionContext.getService(RemoteMessageDispatcherRegistry.class);

        webService.registerController(new ClientController(transferProcessManager));
        webService.registerController(new ClientControlCatalogApiController(remoteMessageDispatcherRegistry));

        /*
         * Registers a API-Key authentication filter
         */
        HttpApiKeyAuthContainerRequestFilter httpApiKeyAuthContainerRequestFilter = new HttpApiKeyAuthContainerRequestFilter(
                resolveApiKeyHeaderName(serviceExtensionContext),
                resolveApiKeyHeaderValue(serviceExtensionContext),
                AuthenticationContainerRequestContextPredicate.INSTANCE);

        webService.registerController(httpApiKeyAuthContainerRequestFilter);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }

    private String resolveApiKeyHeaderName(@NotNull ServiceExtensionContext context) {
        String key = context.getSetting(EDC_API_CONTROL_AUTH_APIKEY_KEY, null);
        if (key == null) {
            key = EDC_API_CONTROL_AUTH_APIKEY_KEY_DEFAULT;
            monitor.warning(String.format("Settings: No setting found for key '%s'. Using default value '%s'", EDC_API_CONTROL_AUTH_APIKEY_KEY, EDC_API_CONTROL_AUTH_APIKEY_KEY_DEFAULT));
        }
        return key;
    }

    private String resolveApiKeyHeaderValue(@NotNull ServiceExtensionContext context) {
        String value = context.getSetting(EDC_API_CONTROL_AUTH_APIKEY_VALUE, null);
        if (value == null) {
            value = generateRandomString();
            monitor.warning(String.format("Settings: No setting found for key '%s'. Using random value '%s'", EDC_API_CONTROL_AUTH_APIKEY_VALUE, value));
        }
        return value;
    }

    /*
     * Produces twelve characters long sequence in the ascii range of '!' (dec 33) to '~' (dec 126).
     *
     * @return sequence
     */
    private static String generateRandomString() {
        StringBuilder stringBuilder = new SecureRandom().ints('!', ((int) '~' + 1))
                .limit(12).collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append);
        return stringBuilder.toString();
    }

    private enum AuthenticationContainerRequestContextPredicate implements Predicate<ContainerRequestContext> {
        INSTANCE;

        @Override
        public boolean test(ContainerRequestContext containerRequestContext) {
            String path = containerRequestContext.getUriInfo().getPath();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            return path.startsWith("/control");
        }
    }
}
