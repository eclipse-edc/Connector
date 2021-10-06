package org.eclipse.dataspaceconnector.ids.daps;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsIssuer;
import org.eclipse.dataspaceconnector.ids.daps.client.X509CertificateDapsIssuer;
import org.eclipse.dataspaceconnector.ids.daps.sec.CertificateProvider;
import org.eclipse.dataspaceconnector.ids.daps.sec.CertificateProviderImpl;
import org.eclipse.dataspaceconnector.ids.daps.sec.PrivateKeyProvider;
import org.eclipse.dataspaceconnector.ids.daps.sec.PrivateKeyProviderImpl;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DatServiceExtension implements ServiceExtension {
    private static final String NAME = "IDS DAPS extension";

    private static final String[] REQUIRES = {
            "dataspaceconnector:http-client"
    };

    private static final String[] PROVIDES = {
            "edc:ids:daps:client"
    };

    @Override
    public Set<String> provides() {
        return new HashSet<>(Arrays.asList(PROVIDES));
    }

    @Override
    public Set<String> requires() {
        return new HashSet<>(Arrays.asList(REQUIRES));
    }

    @Override
    public void initialize(final ServiceExtensionContext serviceExtensionContext) {
        final Monitor monitor = serviceExtensionContext.getMonitor();

        try {
            registerComponents(serviceExtensionContext);
        } catch (final Exception e) {
            throw new EdcException(e.getMessage(), e);
        }

        monitor.info(String.format("Initialized %s.", NAME));
    }

    private void registerComponents(final ServiceExtensionContext serviceExtensionContext) throws Exception {
        final DapsClient dapsClient = createDapsClient(serviceExtensionContext);
        final DatService datService = new DatServiceImpl(dapsClient);

        serviceExtensionContext.registerService(DatService.class, datService);
    }

    private DapsIssuer dapsIssuer(final ServiceExtensionContext serviceExtensionContext) throws Exception {
        final CertificateProvider certificateProvider = certificateProvider(serviceExtensionContext);
        return new X509CertificateDapsIssuer(certificateProvider);
    }

    private PrivateKeyProvider privateKeyProvider(final ServiceExtensionContext serviceExtensionContext) {
        final KeyPair keyPair = keyPair(serviceExtensionContext);
        return new PrivateKeyProviderImpl(keyPair);
    }

    private KeyPair keyPair(final ServiceExtensionContext serviceExtensionContext) {
        throw new EdcException("Not Implemented");
    }

    private CertificateProvider certificateProvider(final ServiceExtensionContext serviceExtensionContext) {
        final X509Certificate x509Certificate = x509Certificate(serviceExtensionContext);
        return new CertificateProviderImpl(x509Certificate);
    }

    private X509Certificate x509Certificate(final ServiceExtensionContext serviceExtensionContext) {
        throw new EdcException("Not Implemented");
    }

    private DapsClient createDapsClient(final ServiceExtensionContext serviceExtensionContext) throws Exception {
        final URL tokenUrl = getTokenUrl(serviceExtensionContext);
        final PrivateKeyProvider privateKeyProvider = privateKeyProvider(serviceExtensionContext);
        final DapsIssuer dapsIssuer = dapsIssuer(serviceExtensionContext);
        final OkHttpClient httpClient = httpClient(serviceExtensionContext);
        final ObjectMapper objectMapper = objectMapper(serviceExtensionContext);

        return new DapsClient(
                tokenUrl, privateKeyProvider, dapsIssuer, httpClient, objectMapper
        );
    }

    private ObjectMapper objectMapper(final ServiceExtensionContext serviceExtensionContext) {
        return Optional.ofNullable(serviceExtensionContext.getService(ObjectMapper.class, true))
                .orElseGet(ObjectMapper::new);
    }

    private URL getTokenUrl(final ServiceExtensionContext serviceExtensionContext) throws MalformedURLException {
        final String url = serviceExtensionContext.getSetting(DatServiceExtensionSettings.EDC_IDS_DAPS_TOKEN_URL, null);

        if (url == null) {
            throw new EdcException(String.format("Configuration %s is required", DatServiceExtensionSettings.EDC_IDS_DAPS_TOKEN_URL));
        }

        return new URL(url);
    }

    private OkHttpClient httpClient(final ServiceExtensionContext serviceExtensionContext) {
        return Optional.ofNullable(serviceExtensionContext.getService(OkHttpClient.class, true))
                .orElseGet(OkHttpClient::new);
    }
}
