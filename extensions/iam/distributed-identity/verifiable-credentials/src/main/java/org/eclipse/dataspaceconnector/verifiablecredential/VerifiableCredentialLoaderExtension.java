package org.eclipse.dataspaceconnector.verifiablecredential;

import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.verifiablecredential.spi.VerifiableCredentialProvider;

import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidConstants.DID_URL_SETTING;

public class VerifiableCredentialLoaderExtension implements ServiceExtension {


    @Override
    public Set<String> provides() {
        return Set.of(VerifiableCredentialProvider.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(PrivateKeyResolver.FEATURE, IonClient.FEATURE, DidPublicKeyResolver.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        monitor.info("Initializing Verifiable Credential");
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        // convenience feature to easily obtain the VC
        VerifiableCredentialProvider verifiableCredentialSupplier = () -> {
            // we'll use the connector name to restore the Private Key
            var connectorName = context.getConnectorId();
            var resolver = context.getService(PrivateKeyResolver.class);
            var privateKeyString = resolver.resolvePrivateKey(connectorName, ECKey.class); //to get the private key

            // we cannot store the VerifiableCredential in the Vault, because it has an expiry date
            // the Issuer claim must contain the DID URL
            return VerifiableCredentialFactory.create(privateKeyString, Map.of(VerifiableCredentialFactory.OWNER_CLAIM, connectorName), didUrl);
        };

        context.registerService(VerifiableCredentialProvider.class, verifiableCredentialSupplier);
    }

    @Override
    public void start() {
        ServiceExtension.super.start();
    }
}
