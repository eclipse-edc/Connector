package org.eclipse.dataspaceconnector.ids.daps;

import java.time.Duration;
import java.time.Instant;

import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

/**
 * Implementation of the {@link IdentityService} interface. Uses the {@link DatService} to obtain client credentials.
 */
public class IdentityServiceImpl implements IdentityService {

    private final DatService datService;
    private final Monitor monitor;

    public IdentityServiceImpl(DatService datService, Monitor monitor) {
        this.datService = datService;
        this.monitor = monitor;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {

        var tokenResultBuilder = TokenResult.Builder.newInstance();

        // TODO exception should not be stored in TokenResult; https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/140
        try {
            var dat = datService.getDat();
            var expiresIn = Duration.between(dat.getExpiration(), Instant.now());
            tokenResultBuilder
                    .token(dat.getToken())
                    .expiresIn(expiresIn.getSeconds());

        } catch (DatServiceException e) {
            monitor.severe("cannot obtain client credentials", e);
            tokenResultBuilder.error(e.getMessage());
        }

        return tokenResultBuilder.build();
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        // TODO take from DAT validation pull request
        throw new RuntimeException("not implemented");
    }

}