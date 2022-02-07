package org.eclipse.dataspaceconnector.token.generation.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGenerationServiceImplTest {

    private TokenGenerationService tokenGenerationService;

    @BeforeEach
    void setUp() throws JOSEException {
        RSAKey testKey = testKey();
        JWSSigner jwsSigner = new RSASSASigner(testKey.toPrivateKey());
        tokenGenerationService = new TokenGenerationServiceImpl(jwsSigner);
    }

    @Test
    void test() throws ParseException {
        var exp = Date.from(Instant.now().plusSeconds(100));
        var result = tokenGenerationService.generate(testClaimSet(exp));

        assertThat(result.succeeded()).isTrue();
        var signedJwt = SignedJWT.parse(result.getContent());

        var claims = signedJwt.getJWTClaimsSet().getClaims();
        assertThat(claims)
                .containsEntry("foo", "bar")
                .containsEntry("exp", Date.from(exp.toInstant().truncatedTo(ChronoUnit.SECONDS)));
    }

    private static JWTClaimsSet testClaimSet(Date exp) {
        return new JWTClaimsSet.Builder()
                .claim("foo", "bar")
                .claim("exp", exp.toInstant().getEpochSecond())
                .build();
    }

    private static RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }
}