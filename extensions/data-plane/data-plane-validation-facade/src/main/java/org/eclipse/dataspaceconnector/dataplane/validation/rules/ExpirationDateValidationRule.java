package org.eclipse.dataspaceconnector.dataplane.validation.rules;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.JwtClaimValidationRule;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;

public class ExpirationDateValidationRule implements JwtClaimValidationRule {

    @Override
    public Result<JWTClaimsSet> checkRule(@NotNull JWTClaimsSet toVerify) {
        Date expiration = toVerify.getExpirationTime();
        if (expiration == null) {
            return Result.failure("Missing expiration time in token");
        }

        // check contract expiration date
        if (Instant.now().isAfter(expiration.toInstant())) {
            return Result.failure("Token has expired");
        }

        return Result.success(toVerify);
    }
}
