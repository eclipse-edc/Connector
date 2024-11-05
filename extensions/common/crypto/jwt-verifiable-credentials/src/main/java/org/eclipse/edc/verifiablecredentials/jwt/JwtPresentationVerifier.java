/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.identitytrust.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.identitytrust.spi.verification.VerifierContext;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes the cryptographic integrity of a VerifiablePresentation when it's represented as JWT. Internally, for the actual
 * cryptographic computation it uses the generic {@link TokenValidationService} together with {@link TokenValidationRule} objects
 * taken from a {@link TokenValidationRulesRegistry}. The main task of <em>this</em> class is to read the JWT,
 * determine whether it's a VP or a VC and parse the contents.
 * <p>
 * In order to be successfully verified, a VP-JWT must contain a "vp" claim, that contains a JSON structure containing a
 * "verifiableCredentials" object.
 * This object contains an array of strings, each representing one VerifiableCredential, represented in JWT format.
 * <p><br/>
 * Both VP-JWTs and VC-JWTs must fulfill the following requirements:
 *
 * <ul>
 *     <li>contain a JWS, that can be verified using the issuer's public key</li>
 *     <li>have in its header a key-id ("kid"), which references the public key in the DID document of the VP-issuer</li>
 *     <li>contain the following claims:
 *     <ul>
 *         <li>aud: must be equal to this connector's DID</li>
 *         <li>sub: only presence is asserted</li>
 *         <li>iss: is used to resolve the DID, that contains the key-id</li>
 *     </ul>
 *     <li>A VP is only verified, if it and all VCs it contains are verified</li>
 * </ul>
 *
 * <em>Note: VP-JWTs may only contain VCs also represented in JWT format. Mixing formats is not allowed.</em>
 */
public class JwtPresentationVerifier implements CredentialVerifier {

    public static final String JWT_VC_TOKEN_CONTEXT = "dcp-vc";
    public static final String JWT_VP_TOKEN_CONTEXT = "dcp-vp";
    public static final String VERIFIABLE_CREDENTIAL_JSON_KEY = "verifiableCredential";
    public static final String VP_CLAIM = "vp";
    public static final String VC_CLAIM = "vc";
    private final ObjectMapper objectMapper;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final PublicKeyResolver publicKeyResolver;

    /**
     * Verifies the JWT presentation by checking the cryptographic integrity.
     */
    public JwtPresentationVerifier(ObjectMapper objectMapper, TokenValidationService tokenValidationService, TokenValidationRulesRegistry tokenValidationRulesRegistry, PublicKeyResolver publicKeyResolver) {
        this.objectMapper = objectMapper;
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.publicKeyResolver = publicKeyResolver;
    }


    @Override
    public boolean canHandle(String rawInput) {
        try {
            SignedJWT.parse(rawInput);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Verifies the presentation by checking the cryptographic integrity as well as the presence of mandatory claims in the JWT.
     *
     * @param serializedJwt The serialized JWT presentation to be verified.
     * @return A Result object representing the verification result, containing specific error messages in case of failure.
     */
    @Override
    public Result<Void> verify(String serializedJwt, VerifierContext context) {

        // verify the "outer" JWT, i.e. the VP JWT

        Result<ClaimToken> verificationResult;

        // verify all "inner" VC JWTs
        try {
            // obtain the actual JSON structure
            var signedJwt = SignedJWT.parse(serializedJwt);
            var keyId = signedJwt.getHeader().getKeyID();
            if (isCredential(signedJwt)) {
                var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(JWT_VC_TOKEN_CONTEXT));
                rules.add(new IssuerKeyIdValidationRule(keyId));
                return tokenValidationService.validate(serializedJwt, publicKeyResolver, rules)
                        .mapEmpty();
            }

            if (!isPresentation(signedJwt)) {
                return Result.failure("Either '%s' or '%s' claim must be present in JWT.".formatted(VP_CLAIM, VC_CLAIM));
            }

            //we can be sure to have a presentation token
            verificationResult = tokenValidationService.validate(serializedJwt, publicKeyResolver, vpValidationRules(context.getAudience(), keyId));

            var vpClaim = (Map<String, Object>) signedJwt.getJWTClaimsSet().getClaim(VP_CLAIM);

            if (!vpClaim.containsKey(VERIFIABLE_CREDENTIAL_JSON_KEY)) {
                return Result.failure("Presentation object did not contain mandatory object: " + VERIFIABLE_CREDENTIAL_JSON_KEY);
            }
            var rawCredentials = extractCredentials(vpClaim.get(VERIFIABLE_CREDENTIAL_JSON_KEY));

            if (rawCredentials.isEmpty()) {
                // todo: this is allowed by the spec, but it is semantic nonsense. Should we return failure or not?
                return Result.success();
            }

            // every VC is represented as another JWT, so we verify all of them
            for (var token : rawCredentials) {
                verificationResult = verificationResult.merge(context.toBuilder().audience(signedJwt.getJWTClaimsSet().getIssuer()).build().verify(token));
            }

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return verificationResult.mapEmpty();
    }

    private List<TokenValidationRule> vpValidationRules(String audience, String keyId) {

        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(JWT_VP_TOKEN_CONTEXT));
        rules.add(new IssuerKeyIdValidationRule(keyId));
        return Optional.ofNullable(audience)
                .map(aud -> {
                    List<TokenValidationRule> r = new ArrayList<>(rules);
                    var audRule = new AudienceValidationRule(audience);
                    r.add(audRule);
                    return r;
                })
                .orElse(rules);

    }

    private boolean isCredential(SignedJWT jwt) throws ParseException {
        return jwt.getJWTClaimsSet().getClaims().containsKey(VC_CLAIM);
    }

    private boolean isPresentation(SignedJWT jwt) throws ParseException {
        return jwt.getJWTClaimsSet().getClaims().containsKey(VP_CLAIM);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCredentials(Object credentialsObject) {
        if (credentialsObject instanceof Collection<?>) {
            return ((Collection) credentialsObject).stream().map(obj -> {
                try {
                    return (obj instanceof String) ? obj.toString() : objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        }
        return List.of(credentialsObject.toString());
    }
}
