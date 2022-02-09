package org.eclipse.dataspaceconnector.token.validation.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.spi.TokenValidationService;
import org.eclipse.dataspaceconnector.token.spi.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenValidationServiceImplTest {

    private TokenValidationService tokenValidationService;
    private RSAKey key;
    private ValidationRule rule;
    private Date now;

    @BeforeEach
    public void setUp() throws JOSEException {
        key = testKey();
        rule = mock(ValidationRule.class);
        tokenValidationService = new TokenValidationServiceImpl(key.toPublicKey(), Collections.singletonList(rule));
        now = new Date();
    }

    @Test
    void validationRuleOk() throws JOSEException {
        var claims = testClaimSet(now);
        when(rule.checkRule(any())).thenReturn(Result.success(claims));

        var result = tokenValidationService.validate(createJwt(claims, key.toPrivateKey()));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims())
                .containsEntry("foo", "bar")
                .containsEntry("exp", now.toString());
    }

    @Test
    void validationRuleKo() throws JOSEException {
        var claims = testClaimSet(now);
        when(rule.checkRule(any())).thenReturn(Result.failure("Rule validation failed!"));

        var result = tokenValidationService.validate(createJwt(claims, key.toPrivateKey()));

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Rule validation failed!");
    }

    private JWTClaimsSet testClaimSet(Date exp) {
        return new JWTClaimsSet.Builder()
                .claim("foo", "bar")
                .expirationTime(exp)
                .build();
    }

    private String createJwt(JWTClaimsSet claimsSet, PrivateKey pk) {
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        try {
            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(new RSASSASigner(pk));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }

    private static RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }
}