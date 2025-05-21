/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.core.validation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.core.IdentityAndTrustExtension;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.identitytrust.spi.TestFunctions.createToken;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(DependencyInjectionExtension.class)
public class SelfIssueIdTokenValidationActionComponentTest {

    public static final String CONSUMER_DID = "did:web:consumer";
    public static final String CONSUMER_DID_KEY = "did:web:consumer#key";
    public static final String EXPECTED_AUDIENCE = "did:web:test";
    private static final String CONNECTOR_DID_PROPERTY = "edc.iam.issuer.id";
    private static final String CLEANUP_PERIOD = "edc.sql.store.jti.cleanup.period";
    private static final ECKeyGenerator EC_KEY_GENERATOR = new ECKeyGenerator(Curve.P_256);
    private final JtiValidationStore storeMock = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final DidPublicKeyResolver publicKeyResolver = mock();
    private final TokenValidationRulesRegistryImpl ruleRegistry = new TokenValidationRulesRegistryImpl();
    private final TokenValidationService tokenValidationService = new TokenValidationServiceImpl();
    private final SelfIssueIdTokenValidationAction verifier = new SelfIssueIdTokenValidationAction(tokenValidationService, ruleRegistry, publicKeyResolver);

    public static ECKey generateEcKey(String kid) {
        try {
            return EC_KEY_GENERATOR.keyUse(KeyUse.SIGNATURE).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(SecureTokenService.class, mock());
        context.registerService(TypeManager.class, new JacksonTypeManager());
        context.registerService(Clock.class, Clock.systemUTC());
        context.registerService(JtiValidationStore.class, storeMock);
        context.registerService(ExecutorInstrumentation.class, ExecutorInstrumentation.noop());
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        context.registerService(TokenValidationRulesRegistry.class, ruleRegistry);
        context.registerService(TokenValidationService.class, tokenValidationService);

        var config = ConfigFactory.fromMap(Map.of(
                CONNECTOR_DID_PROPERTY, EXPECTED_AUDIENCE,
                CLEANUP_PERIOD, "1"
        ));
        when(context.getConfig()).thenReturn(config);
    }

    @Test
    void verify_invalidToken(ServiceExtensionContext context, IdentityAndTrustExtension extension) {
        extension.initialize(context);
        assertThatThrownBy(() -> verifier.apply(TokenRepresentation.Builder.newInstance().token("token").build()))
                .isInstanceOf(EdcException.class)
                .hasCauseInstanceOf(ParseException.class);
    }

    @Test
    void verify(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);
        var key = generateEcKey(CONSUMER_DID_KEY);
        var claims = new JWTClaimsSet.Builder()
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(EXPECTED_AUDIENCE)
                .claim("token", "accessToken")
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .build();

        when(publicKeyResolver.resolveKey(CONSUMER_DID_KEY)).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isSucceeded();
    }


    @Test
    void verify_whenIssuerMismatchKid(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);

        var key = generateEcKey(CONSUMER_DID_KEY);
        var claims = new JWTClaimsSet.Builder()
                .issuer("wrongIssuer")
                .subject("wrongIssuer")
                .audience(EXPECTED_AUDIENCE)
                .claim("token", "accessToken")
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .build();

        when(publicKeyResolver.resolveKey(CONSUMER_DID_KEY)).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isFailed().detail()
                .isEqualTo("kid header 'did:web:consumer#key' expected to correlate to 'iss' claim ('wrongIssuer'), but it did not.");
    }

    @Test
    void verify_whenIssuerMismatchSubject(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);

        var key = generateEcKey("did:web:issuer#key");
        var claims = new JWTClaimsSet.Builder()
                .issuer("did:web:issuer")
                .subject("wrongIssuer")
                .audience(EXPECTED_AUDIENCE)
                .claim("token", "accessToken")
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .build();

        when(publicKeyResolver.resolveKey("did:web:issuer#key")).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isFailed().detail()
                .isEqualTo("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    @Test
    void verify_whenUnexpectedAudience(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);

        var key = generateEcKey(CONSUMER_DID_KEY);
        var claims = new JWTClaimsSet.Builder()
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience("unexpectedAudience")
                .claim("token", "accessToken")
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .build();

        when(publicKeyResolver.resolveKey(CONSUMER_DID_KEY)).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isFailed().detail()
                .isEqualTo("Token audience claim (aud -> [unexpectedAudience]) did not contain expected audience: did:web:test");
    }

    @Test
    void verify_whenMissingAccessToken(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);

        var key = generateEcKey(CONSUMER_DID_KEY);
        var claims = new JWTClaimsSet.Builder()
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(EXPECTED_AUDIENCE)
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .build();

        when(publicKeyResolver.resolveKey(CONSUMER_DID_KEY)).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isFailed().detail()
                .isEqualTo("The 'token' claim is mandatory and must not be null.");
    }

    @Test
    void verify_whenMissingExpiration(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);

        var key = generateEcKey(CONSUMER_DID_KEY);
        var claims = new JWTClaimsSet.Builder()
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(EXPECTED_AUDIENCE)
                .build();

        when(publicKeyResolver.resolveKey(CONSUMER_DID_KEY)).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isFailed().detail()
                .isEqualTo("Required expiration time (exp) claim is missing in token, The 'token' claim is mandatory and must not be null.");
    }

    @Test
    void verify_whenSubJwkIsNotNull(ServiceExtensionContext context, IdentityAndTrustExtension extension) throws JOSEException {
        extension.initialize(context);

        var key = generateEcKey(CONSUMER_DID_KEY);
        var claims = new JWTClaimsSet.Builder()
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(EXPECTED_AUDIENCE)
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .claim("token", "accessToken")
                .claim("sub_jwk", "sub_jwk")
                .build();

        when(publicKeyResolver.resolveKey(CONSUMER_DID_KEY)).thenReturn(Result.success(key.toPublicKey()));
        var token = createToken(claims, key);
        assertThat(verifier.apply(token)).isFailed().detail()
                .isEqualTo("The 'sub_jwk' claim must not be present.");
    }

}
