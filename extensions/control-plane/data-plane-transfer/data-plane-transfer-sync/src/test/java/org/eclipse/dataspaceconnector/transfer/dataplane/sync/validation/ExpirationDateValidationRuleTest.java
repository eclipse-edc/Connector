package org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class ExpirationDateValidationRuleTest {

    private final Instant now = Instant.now();
    private final ExpirationDateValidationRule rule = new ExpirationDateValidationRule(Clock.fixed(now, UTC));

    @Test
    void failsWithoutExpiration() {
        var token = ClaimToken.Builder.newInstance().build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result).matches(Result::failed)
                .extracting(Result::getFailureDetail).isEqualTo("Missing expiration time in token");
    }

    @Test
    void failsIfTokenIsExpired() {
        var token = ClaimToken.Builder.newInstance()
                .claim("exp", Date.from(now))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result).matches(Result::failed)
                .extracting(Result::getFailureDetail).asString().startsWith("Token has expired on");
    }

    @Test
    void succeedIfTokenIsNotExpired() {
        var token = ClaimToken.Builder.newInstance()
                .claim("exp", Date.from(now.plusMillis(1)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result).matches(Result::succeeded);
    }
}