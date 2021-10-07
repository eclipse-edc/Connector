package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Oauth2ValidationRuleTest {

    public static final String TEST_AUDIENCE = "test-audience";
    private Oauth2ValidationRule rule;
    private Instant now;

    @Test
    void validationKoBecauseNotBeforeTimeNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now.plusSeconds(20)))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("Token is not valid yet");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("Missing notBefore time in token claims");
    }

    @Test
    void validationKoBecauseExpirationTimeNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.minusSeconds(10)))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("Token has expired");
    }

    @Test
    void validationKoBecauseExpirationTimeNotProvided() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("Missing expiration time in token claims");
    }

    @Test
    void validationKoBecauseAudienceNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("fake-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("Token audience did not match required audience: test-audience");
    }

    @Test
    void validationKoBecauseAudienceNotProvided() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("Missing audience in token claims");
    }

    @Test
    void validationOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        ValidationRuleResult result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.isSuccess()).isTrue();
    }

    @BeforeEach
    public void setUp() {
        rule = new Oauth2ValidationRule();
        now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}