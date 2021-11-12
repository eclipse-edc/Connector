package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import java.util.Objects;
import java.util.Set;

import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

public class IdsMultipartClientServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_ID = "edc.ids.id";
    public static final String DEFAULT_EDC_IDS_ID = "urn:connector:edc";

    private static final String NAME = "IDS Multipart Client API extension";

    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of(IdentityService.FEATURE,
                "edc:ids:transform:v1",
                "dataspaceconnector:dispatcher");
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:ids:api:multipart:dispatcher:v1");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var connectorId = resolveConnectorId(context);
        var httpClient = context.getService(OkHttpClient.class);
        var identityService = context.getService(IdentityService.class);

        var serializer = new Serializer();

        var multipartDispatcher = new IdsMultipartRemoteMessageDispatcher();
        multipartDispatcher.register(new MultipartDescriptionRequestSender(connectorId, httpClient, serializer, monitor, identityService));

        var registry = context.getService(RemoteMessageDispatcherRegistry.class);
        registry.register(multipartDispatcher);

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

    private String resolveConnectorId(@NotNull ServiceExtensionContext context) {
        Objects.requireNonNull(context);

        String value = context.getSetting(EDC_IDS_ID, null);

        if (value == null) {
            String message = "IDS Settings: No setting found for key '%s'. Using default value '%s'";
            monitor.warning(String.format(message, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
            value = DEFAULT_EDC_IDS_ID;
        }

        try {
            // Hint: use stringified uri to keep uri path and query
            IdsId idsId = IdsIdParser.parse(value);
            if (idsId != null && idsId.getType() == IdsType.CONNECTOR) {
                return idsId.getValue();
            }
        } catch (IllegalArgumentException e) {
            String message = "IDS Settings: Expected valid URN for setting '%s', but was %s'. Expected format: 'urn:connector:[id]'";
            throw new EdcException(String.format(message, EDC_IDS_ID, DEFAULT_EDC_IDS_ID));
        }

        return value;
    }

}
