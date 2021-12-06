package org.eclipse.dataspaceconnector.iam.oauth2.core.impl;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.ValidationRule;
import org.eclipse.dataspaceconnector.spi.Result;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class Oauth2ValidationRule implements ValidationRule {

    private static Instant convertToUtcTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")).toInstant();
    }

    /**
     * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
     */
    @Override
    public Result<JWTClaimsSet> checkRule(JWTClaimsSet toVerify, String audience) {
        Instant nowUtc = Instant.now();
        List<String> errors = new ArrayList<>();

        // check audiences
        List<String> audiences = toVerify.getAudience();
        if (audiences.isEmpty()) {
            errors.add("Missing audience in token claims");
        } else if (!audiences.contains(audience)) {
            errors.add("Token audience did not match required audience: " + audience);
        }

        // check not before
        var notBefore = toVerify.getNotBeforeTime();
        if (notBefore == null) {
            errors.add("Missing notBefore time in token claims");
        } else if (nowUtc.isBefore(convertToUtcTime(notBefore))) {
            errors.add("Token is not valid yet");
        }

        // check expiration time
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
}
