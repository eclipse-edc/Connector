package org.eclipse.dataspaceconnector.ids.daps;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClient;
import org.eclipse.dataspaceconnector.ids.daps.client.IssuerProvider;
import org.eclipse.dataspaceconnector.ids.daps.client.X509CertificateIssuerProvider;
import org.eclipse.dataspaceconnector.ids.daps.sec.CertificateProvider;
import org.eclipse.dataspaceconnector.ids.daps.sec.CertificateProviderImpl;
import org.eclipse.dataspaceconnector.ids.daps.sec.PrivateKeyProvider;
import org.eclipse.dataspaceconnector.ids.daps.sec.PrivateKeyProviderImpl;
import org.eclipse.dataspaceconnector.ids.daps.sec.pem.KeyPairPemReader;
import org.eclipse.dataspaceconnector.ids.daps.sec.pem.X509CertificatePemReader;
import org.eclipse.dataspaceconnector.ids.spi.configuration.ConfigurationProvider;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DynamicAttributeProvisionServiceExtension implements ServiceExtension {
    private static final String NAME = "IDS DAPS extension";

    private static final String[] REQUIRES = {
            "dataspaceconnector:http-client"
    };

    private static final String[] PROVIDES = {
            "edc:ids:daps:client",
            IdentityService.FEATURE
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

    private void registerComponents(final ServiceExtensionContext serviceExtensionContext) {
        final DapsClient dapsClient = createDapsClient(serviceExtensionContext);
        final DynamicAttributeTokenProvider dynamicAttributeTokenProvider = new DynamicAttributeTokenProviderImpl(dapsClient);
        final IdentityService identityService = new IdentityServiceImpl(dynamicAttributeTokenProvider, serviceExtensionContext.getMonitor());

        serviceExtensionContext.registerService(IdentityService.class, identityService);
    }

    private IssuerProvider issuerProvider(final ConfigurationProvider configurationProvider) {
        final CertificateProvider certificateProvider = certificateProvider(configurationProvider);
        return new X509CertificateIssuerProvider(certificateProvider);
    }

    private PrivateKeyProvider privateKeyProvider(final ConfigurationProvider configurationProvider) {
        final KeyPair keyPair = keyPair(configurationProvider);
        return new PrivateKeyProviderImpl(keyPair);
    }

    // TODO works only if the certificate is in PEM format
    private KeyPair keyPair(final ConfigurationProvider configurationProvider) {
        final Path keyPairPath = configurationProvider.resolveKeyPairPath();
        final Optional<String> keyPairPassphrase = configurationProvider.resolveKeyPairPassphrase();

        try (final InputStream inputstream = Files.newInputStream(keyPairPath)) {
            final KeyPairPemReader reader = new KeyPairPemReader();
            return reader.readKeyPair(inputstream, keyPairPassphrase.map(String::toCharArray).orElse(null));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
            throw new EdcException(e);
        }
    }

    private CertificateProvider certificateProvider(final ConfigurationProvider configurationProvider) {
        final X509Certificate x509Certificate = x509Certificate(configurationProvider);
        return new CertificateProviderImpl(x509Certificate);
    }

    // TODO works only if the certificate is in PEM format
    private X509Certificate x509Certificate(ConfigurationProvider configurationProvider) {
        final Path certificatePath = configurationProvider.resolveCertificatePath();
        final X509CertificatePemReader reader = new X509CertificatePemReader();

        try (final InputStream inputstream = Files.newInputStream(certificatePath)) {
            return reader.readX509Certificate(inputstream);
        } catch (IOException | CertificateException e) {
            throw new EdcException(e);
        }
    }

    private DapsClient createDapsClient(final ServiceExtensionContext serviceExtensionContext) {
        final ConfigurationProvider configurationProvider = serviceExtensionContext.getService(ConfigurationProvider.class);

        final URL tokenUrl = configurationProvider.resolveDynamicAttributeProvisionServiceTokenUrl();
        final PrivateKeyProvider privateKeyProvider = privateKeyProvider(configurationProvider);
        final IssuerProvider issuerProvider = issuerProvider(configurationProvider);
        final OkHttpClient httpClient = httpClient(serviceExtensionContext);
        final ObjectMapper objectMapper = objectMapper(serviceExtensionContext);

        return new DapsClient(tokenUrl, privateKeyProvider, issuerProvider, httpClient, objectMapper);
    }

    private ObjectMapper objectMapper(final ServiceExtensionContext serviceExtensionContext) {
        return Optional.ofNullable(serviceExtensionContext.getService(ObjectMapper.class, true))
                .orElseGet(ObjectMapper::new);
    }

    private OkHttpClient httpClient(final ServiceExtensionContext serviceExtensionContext) {
        return Optional.ofNullable(serviceExtensionContext.getService(OkHttpClient.class, true))
                .orElseGet(OkHttpClient::new);
    }
}
