package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.DID_URL_SETTING;

public class DistributedIdentityServiceExtension implements ServiceExtension {
    @Override
    public Set<String> provides() {
        return Set.of(IdentityService.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(DidResolverRegistry.FEATURE, CredentialsVerifier.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vcProvider = createSupplier(context);
        var resolverRegistry = context.getService(DidResolverRegistry.class);
        var credentialsVerifier = context.getService(CredentialsVerifier.class);
        var identityService = new DistributedIdentityService(vcProvider, resolverRegistry, credentialsVerifier, context.getMonitor());
        context.registerService(IdentityService.class, identityService);

        context.getMonitor().info("Initialized Distributed Identity Service extension");

    }

    Supplier<SignedJWT> createSupplier(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        return () -> {
            // we'll use the connector name to restore the Private Key
            var connectorName = context.getConnectorId();
            var resolver = context.getService(PrivateKeyResolver.class);
            var privateKeyString = resolver.resolvePrivateKey(connectorName, ECKey.class); //to get the private key

            // we cannot store the VerifiableCredential in the Vault, because it has an expiry date
            // the Issuer claim must contain the DID URL
            return VerifiableCredentialFactory.create(privateKeyString, Map.of(VerifiableCredentialFactory.OWNER_CLAIM, connectorName), didUrl);
        };
    }

    @Override
    public void start() {
        ServiceExtension.super.start();
    }
}
