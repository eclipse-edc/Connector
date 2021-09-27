package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.JwkPublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.VerificationMethod;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.verifiablecredential.KeyConverter;
import org.eclipse.dataspaceconnector.verifiablecredential.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.verifiablecredential.spi.VerifiableCredentialProvider;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DistributedIdentityService implements IdentityService {

    private final VerifiableCredentialProvider verifiableCredentialProvider;
    private final DidResolver didResolver;
    private final CredentialsVerifier credentialsVerifier;
    private final Monitor monitor;

    public DistributedIdentityService(VerifiableCredentialProvider vcProvider, DidResolver didResolver, CredentialsVerifier credentialsVerifier, Monitor monitor) {
        verifiableCredentialProvider = vcProvider;
        this.didResolver = didResolver;
        this.credentialsVerifier = credentialsVerifier;
        this.monitor = monitor;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {

        var jwt = verifiableCredentialProvider.get();
        var token = jwt.serialize();
        var expiration = new Date().getTime() + TimeUnit.MINUTES.toMillis(10);

        return TokenResult.Builder.newInstance().token(token).expiresIn(expiration).build();
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        try {
            var jwt = SignedJWT.parse(token);
            monitor.debug("Starting verification...");

            monitor.debug("Resolving other party's DID Document");
            var did = didResolver.resolve(jwt.getJWTClaimsSet().getIssuer());
            monitor.debug("Extracting public key");

            // this will return the _first_ public key entry
            Optional<VerificationMethod> publicKey = getPublicKey(did);
            if (publicKey.isEmpty()) {
                return new VerificationResult("Public Key not found in DID Document!");
            }

            //convert the POJO into a usable PK-wrapper:
            JwkPublicKey publicKeyJwk = publicKey.get().getPublicKeyJwk();
            PublicKeyWrapper publicKeyWrapper = KeyConverter.toPublicKeyWrapper(publicKeyJwk, publicKey.get().getId());

            monitor.debug("Verifying JWT with public key...");
            if (!VerifiableCredentialFactory.verify(jwt, publicKeyWrapper)) {
                return new VerificationResult("Token could not be verified!");
            }
            monitor.debug("verification successful! Fetching data from IdentityHub");
            String hubUrl = getHubUrl(did);
            var credentialsResult = credentialsVerifier.verifyCredentials(hubUrl, publicKeyWrapper);

            monitor.debug("Building ClaimToken");
            var tokenBuilder = ClaimToken.Builder.newInstance();
            var claimToken = tokenBuilder.claims(credentialsResult.getValidatedCredentials()).build();

            return new VerificationResult(claimToken);
        } catch (ParseException e) {
            monitor.info("Error parsing JWT", e);
            return new VerificationResult("Error parsing JWT");
        }
    }

    String getHubUrl(DidDocument did) {
        return did.getService().stream().filter(service -> service.getType().equals(DidConstants.HUB_URL)).map(Service::getServiceEndpoint).findFirst().orElseThrow();
    }

    @NotNull
    private Optional<VerificationMethod> getPublicKey(DidDocument did) {
        return did.getVerificationMethod().stream().filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).findFirst();
    }
}
