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
 *       Think-it GmbH - presentation format detection
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.JwtPresentationFormat;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.PresentationFormatRule;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
 * A VP-JWT must be a VCDM 1.1 presentation as determined by {@link JwtPresentationFormat}, and the documents it contains
 * must be credentials.
 *
 * <em>Note: VP-JWTs may only contain VCs also represented in JWT format. Mixing formats is not allowed.</em>
 */
public class JwtPresentationVerifier implements CredentialVerifier {

    private final TypeManager typeManager;
    private final String typeContext;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final PublicKeyResolver publicKeyResolver;

    /**
     * Verifies the JWT presentation by checking the cryptographic integrity.
     */
    public JwtPresentationVerifier(TypeManager typeManager, String typeContext, TokenValidationService tokenValidationService, TokenValidationRulesRegistry tokenValidationRulesRegistry, PublicKeyResolver publicKeyResolver) {
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.publicKeyResolver = publicKeyResolver;
    }


    @Override
    public boolean canHandle(String rawInput) {
        try {
            var claims = SignedJWT.parse(rawInput).getJWTClaimsSet().getClaims();
            return isCredential(claims) || JwtPresentationFormat.of(claims) == CredentialFormat.VC1_0_JWT;
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

        try {
            var signedJwt = SignedJWT.parse(serializedJwt);
            var keyId = signedJwt.getHeader().getKeyID();
            var jwtClaimsSet = signedJwt.getJWTClaimsSet();
            var claims = jwtClaimsSet.getClaims();

            if (isCredential(claims)) {
                return validateAsCredential(serializedJwt, keyId);
            }

            if (context.isCredentialExpected()) {
                return Result.failure("Expected a credential, but the JWT contains a presentation.");
            }

            if (isPresentation(jwtClaimsSet)) {
                return validateAsPresentation(serializedJwt, keyId, context, jwtClaimsSet);
            }

            return Result.failure("Either '%s' or '%s' claim must be present in JWT.".formatted(Constants.VP_CLAIM, Constants.VC_CLAIM));

        } catch (ParseException e) {
            return Result.failure("Cannot parse JWT: " + e.getMessage());
        }
    }

    private Result<Void> validateAsPresentation(String serializedJwt, String keyId, VerifierContext context, JWTClaimsSet claimsSet) {
        var verificationResult = tokenValidationService.validate(serializedJwt, publicKeyResolver, vpValidationRules(context.getAudience(), keyId));
        if (verificationResult.failed()) {
            // the credentials of a presentation that is not valid are not verified
            return verificationResult.mapFailure();
        }

        var vpClaim = vpClaim(claimsSet.getClaim(Constants.VP_CLAIM));
        if (vpClaim == null) {
            return Result.failure("The '%s' claim must be a JSON object.".formatted(Constants.VP_CLAIM));
        }

        if (!vpClaim.containsKey(Constants.VERIFIABLE_CREDENTIAL_JSON_KEY)) {
            return Result.failure("Presentation object did not contain mandatory object: " + Constants.VERIFIABLE_CREDENTIAL_JSON_KEY);
        }
        var rawCredentials = extractCredentials(vpClaim.get(Constants.VERIFIABLE_CREDENTIAL_JSON_KEY));

        if (rawCredentials.isEmpty()) {
            return Result.success();
        }

        // every VC is represented as another JWT or as a JSON-LD document
        var credentialContext = context.toBuilder()
                .audience(claimsSet.getIssuer())
                .credentialExpected(true)
                .build();

        return rawCredentials.stream().map(credentialContext::verify).reduce(Result.success(), Result::merge);
    }

    private Result<Void> validateAsCredential(String serializedJwt, String keyId) {
        var rules = CredentialValidationRules.vcdm11(tokenValidationRulesRegistry, keyId);
        return tokenValidationService.validate(serializedJwt, publicKeyResolver, rules)
                .mapEmpty();
    }

    /**
     * Returns the {@code vp} claim as a map. The claim is either a JSON object, or a string that contains one.
     *
     * @return the claim as map, or null if it is not a JSON object
     */
    @SuppressWarnings("unchecked")
    private @Nullable Map<String, Object> vpClaim(Object vpClaim) {
        if (vpClaim instanceof String vpString) {
            try {
                return typeManager.getMapper(typeContext).readValue(vpString, Map.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return vpClaim instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private List<TokenValidationRule> vpValidationRules(String audience, String keyId) {
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(Constants.JWT_VP_TOKEN_CONTEXT));
        rules.add(new IssuerKeyIdValidationRule(keyId));
        rules.add(new PresentationFormatRule(CredentialFormat.VC1_0_JWT));
        if (audience != null) {
            rules.add(new AudienceValidationRule(audience));
        }

        return rules;
    }

    private boolean isCredential(Map<String, Object> claims) {
        return claims.containsKey(Constants.VC_CLAIM) && !claims.containsKey(Constants.VP_CLAIM);
    }

    private boolean isPresentation(JWTClaimsSet jwtClaimsSet) {
        return jwtClaimsSet.getClaim(Constants.VP_CLAIM) != null;
    }

    private List<String> extractCredentials(Object credentialsObject) {
        if (credentialsObject instanceof Collection<?> credentialsCollection) {
            return credentialsCollection.stream().map(obj -> {
                try {
                    return (obj instanceof String stringCredential) ? stringCredential : typeManager.getMapper(typeContext).writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        }
        return List.of(credentialsObject.toString());
    }
}
