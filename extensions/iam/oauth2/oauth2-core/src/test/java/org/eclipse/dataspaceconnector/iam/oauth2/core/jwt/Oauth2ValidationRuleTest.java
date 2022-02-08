package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Oauth2ValidationRuleTest {

    public static final String TEST_AUDIENCE = "test-audience";
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private Oauth2ValidationRule rule;

    @BeforeEach
    public void setUp() {
        var configuration = Oauth2Configuration.Builder.newInstance().build();
        rule = new Oauth2ValidationRule(configuration);
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now.plusSeconds(20)))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token is not valid yet");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Missing notBefore time in token claims");
    }

    @Test
    void validationKoBecauseExpirationTimeNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.minusSeconds(10)))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token has expired");
    }

    @Test
    void validationKoBecauseExpirationTimeNotProvided() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Missing expiration time in token claims");
    }

    @Test
    void validationKoBecauseAudienceNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("fake-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token audience did not match required audience: test-audience");
    }

    @Test
    void validationKoBecauseAudienceNotProvided() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Missing audience in token claims");
    }

    @Test
    void validationOkWhenLeewayOnNotBefore() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now.plusSeconds(20)))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();
        var configuration = Oauth2Configuration.Builder.newInstance()
                .notBeforeValidationLeeway(20)
                .build();
        rule = new Oauth2ValidationRule(configuration);

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationOk() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(claims, TEST_AUDIENCE);

        assertThat(result.succeeded()).isTrue();
    }
}