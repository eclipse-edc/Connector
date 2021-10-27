/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.eclipse.dataspaceconnector.iam.oauth2.impl.Fingerprint.sha1Base64Fingerprint;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {
    private static final long EXPIRATION = 300; // 5 minutes

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Oauth2Configuration configuration;

    private final List<ValidationRule> validationRules;
    private final JWSSigner tokenSigner;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param configuration             The configuration
     * @param signerProvider            A {@link Supplier} which is used to get a {@link JWSSigner} instance.
     * @param additionalValidationRules An optional list of {@link ValidationRule} that are evaluated <em>after</em> the
     *                                  standard OAuth2 validation
     */
    public Oauth2ServiceImpl(Oauth2Configuration configuration, Supplier<JWSSigner> signerProvider, ValidationRule... additionalValidationRules) {
        this.configuration = configuration;

        List<ValidationRule> rules = new ArrayList<>();
        rules.add(new Oauth2ValidationRule()); //OAuth2 validation must ALWAYS be done
        rules.addAll(List.of(additionalValidationRules));
        validationRules = Collections.unmodifiableList(rules);

        tokenSigner = signerProvider.get();
        if (tokenSigner == null) {
            throw new EdcException("Could not resolve private key");
        }
    }
    
    @Override
    public TokenResult obtainClientCredentials(String scope) {
        String assertion = buildJwt(configuration.getProviderAudience());

        RequestBody requestBody = new FormBody.Builder()
                .add("client_assertion_type", ASSERTION_TYPE)
                .add("grant_type", GRANT_TYPE)
                .add("client_assertion", assertion)
                .add("scope", scope)
                .build();

        Request request = new Request.Builder().url(configuration.getTokenUrl()).addHeader("Content-Type", CONTENT_TYPE).post(requestBody).build();

        OkHttpClient client = createClient();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (var body = response.body()) {
                    String message = body == null ? "<empty body>" : body.string();
                    return TokenResult.Builder.newInstance().error(message).build();
                }
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return TokenResult.Builder.newInstance().error("<empty token body>").build();
            }

            String responsePayload = responseBody.string();
            LinkedHashMap<String, Object> deserialized = configuration.getObjectMapper().readValue(responsePayload, LinkedHashMap.class);
            String token = (String) deserialized.get("access_token");
            long expiresIn = ((Integer) deserialized.get("expires_in")).longValue();
            return TokenResult.Builder.newInstance().token(token).expiresIn(expiresIn).build();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        try {
            var signedJwt = SignedJWT.parse(token);

            var verifier = createVerifier(signedJwt.getHeader(), configuration.getPublicCertificateAlias());
            if (verifier == null) {
                return new VerificationResult("Token verification not successful");
            }

            if (!signedJwt.verify(verifier)) {
                return new VerificationResult("Token verification not successful");
            }
            var claimsSet = signedJwt.getJWTClaimsSet();

            // now we get the results of all the single rules, lets collate them into one
            var res = validationRules.stream()
                    .map(r -> r.checkRule(claimsSet, audience))
                    .reduce(ValidationRuleResult::merge)
                    .orElseThrow();

            // return instantly if there are errors present
            if (!res.isSuccess()) {
                return new VerificationResult(res.getErrorMessages());
            }

            // build claim tokens
            var tokenBuilder = ClaimToken.Builder.newInstance();
            claimsSet.getClaims().forEach((k, v) -> {
                var claimValue = Objects.toString(v);
                if (claimValue == null) {
                    // only support strings
                    return;
                }
                tokenBuilder.claim(k, claimValue);
            });
            return new VerificationResult(tokenBuilder.build());

        } catch (JOSEException e) {
            return new VerificationResult(e.getMessage());
        } catch (ParseException e) {
            return new VerificationResult("Token could not be decoded");
        }
    }


    @Nullable
    private JWSVerifier createVerifier(JWSHeader header, String publicCertificateAlias) {
        var publicKey = Objects.requireNonNull(configuration.getCertificateResolver().resolveCertificate(publicCertificateAlias)).getPublicKey();

        try {
            return new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
        } catch (JOSEException e) {
            return null;
        }
    }

    private String buildJwt(String providerAudience) {
        try {
            X509Certificate certificate = configuration.getCertificateResolver().resolveCertificate(configuration.getPublicCertificateAlias());
            if (certificate == null) {
                throw new EdcException("Public certificate not found: " + configuration.getPublicCertificateAlias());
            }


            JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256);
            headerBuilder.customParam("x5t", sha1Base64Fingerprint(certificate.getEncoded()));
            var claimsSet = new JWTClaimsSet.Builder();
            claimsSet.audience(providerAudience)
                    .issuer(configuration.getClientId())
                    .subject(configuration.getClientId())
                    .jwtID(UUID.randomUUID().toString())
                    .notBeforeTime(new Date()) //now
                    .expirationTime(Date.from(Instant.now().plusSeconds(EXPIRATION)));
            var jwt = new SignedJWT(headerBuilder.build(), claimsSet.build());
            jwt.sign(tokenSigner);
            return jwt.serialize();
        } catch (GeneralSecurityException | JOSEException e) {
            throw new EdcException(e);
        }
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    }

}
