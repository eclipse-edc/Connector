package org.eclipse.dataspaceconnector.ids.daps;

import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of the {@link IdentityService} interface. Uses the {@link DynamicAttributeTokenProvider} to obtain client credentials.
 */
public class IdentityServiceImpl implements IdentityService {

    private final DynamicAttributeTokenProvider dynamicAttributeTokenProvider;
    private final Monitor monitor;

    public IdentityServiceImpl(final DynamicAttributeTokenProvider dynamicAttributeTokenProvider, final Monitor monitor) {
        this.dynamicAttributeTokenProvider = dynamicAttributeTokenProvider;
        this.monitor = monitor;
    }

    @Override
    // TODO do something with scope
    public TokenResult obtainClientCredentials(final String scope) {
        try {
            final DynamicAttributeToken dynamicAttributeToken = dynamicAttributeTokenProvider.getDynamicAttributeToken();
            final Duration expiresIn = Duration.between(dynamicAttributeToken.getExpiration(), Instant.now());
            return TokenResult.Builder.newInstance()
                    .token(dynamicAttributeToken.getToken())
                    .expiresIn(expiresIn.getSeconds()).build();

        } catch (DynamicAttributeTokenException e) {
            monitor.severe("cannot obtain client credentials", e); // last chance for the stacktrace to survive
            // TODO exception should not be stored in TokenResult; https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/140
            return TokenResult.Builder.newInstance().error(e.getMessage()).build();
        }
    }

    @Override
    public VerificationResult verifyJwtToken(final String token, final String audience) {
        // TODO take from DAT validation pull request
        throw new RuntimeException("not implemented");
    }

}