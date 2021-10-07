package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.nimbusds.jwt.JWTClaimsSet;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    public ValidationRuleResult checkRule(JWTClaimsSet toVerify, String audience) {
        Instant nowUtc = Instant.now();
        ValidationRuleResult result = new ValidationRuleResult();

        // check audiences
        List<String> audiences = toVerify.getAudience();
        if (audiences.isEmpty()) {
            result.reportsError("Missing audience in token claims");
        } else if (!audiences.contains(audience)) {
            result.reportsError("Token audience did not match required audience: " + audience);
        }

        // check not before
        var notBefore = toVerify.getNotBeforeTime();
        if (notBefore == null) {
            result.reportsError("Missing notBefore time in token claims");
        } else if (nowUtc.isBefore(convertToUtcTime(notBefore))) {
            result.reportsError("Token is not valid yet");
        }

        // check expiration time
        Date expires = toVerify.getExpirationTime();
        if (expires == null) {
            result.reportsError("Missing expiration time in token claims");
        } else if (nowUtc.isAfter(convertToUtcTime(expires))) {
            result.reportsError("Token has expired");
        }

        return result;
    }
}
