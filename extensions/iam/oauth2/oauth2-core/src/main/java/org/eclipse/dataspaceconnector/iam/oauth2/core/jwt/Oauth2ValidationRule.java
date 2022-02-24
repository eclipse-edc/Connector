package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.JwtClaimValidationRule;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.time.ZoneOffset.UTC;

public class Oauth2ValidationRule implements JwtClaimValidationRule {

    private final String audience;
    private final Oauth2Configuration configuration;

    public Oauth2ValidationRule(@NotNull String audience, @NotNull Oauth2Configuration configuration) {
        this.audience = audience;
        this.configuration = configuration;
    }

    /**
     * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
     */
    @Override
    public Result<JWTClaimsSet> checkRule(@NotNull JWTClaimsSet toVerify) {
        Instant nowUtc = Instant.now();
        List<String> errors = new ArrayList<>();

        List<String> audiences = toVerify.getAudience();
        if (audiences.isEmpty()) {
            errors.add("Missing audience in token claims");
        } else if (!audiences.contains(audience)) {
            errors.add("Token audience did not match required audience: " + audience);
        }

        var leewayNow = nowUtc.plusSeconds(configuration.getNotBeforeValidationLeeway());
        var notBefore = toVerify.getNotBeforeTime();
        if (notBefore == null) {
            errors.add("Missing notBefore time in token claims");
        } else if (leewayNow.isBefore(convertToUtcTime(notBefore))) {
            errors.add("Token is not valid yet");
        }

        Date expires = toVerify.getExpirationTime();
        if (expires == null) {
            errors.add("Missing expiration time in token claims");
        } else if (nowUtc.isAfter(convertToUtcTime(expires))) {
            errors.add("Token has expired");
        }

        if (errors.isEmpty()) {
            return Result.success(toVerify);
        } else {
            return Result.failure(errors);
        }

    }

    private static Instant convertToUtcTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), UTC).toInstant();
    }
}
