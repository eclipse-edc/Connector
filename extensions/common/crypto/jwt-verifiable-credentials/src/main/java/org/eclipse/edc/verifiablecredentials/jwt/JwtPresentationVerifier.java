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

import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.JwtPresentationFormat;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.PresentationFormatRule;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Computes the cryptographic integrity of a VerifiablePresentation when it's represented as JWT. Internally, for the actual
 * cryptographic computation it uses the generic {@link TokenValidationService} together with {@link TokenValidationRule} objects
 * taken from a {@link TokenValidationRulesRegistry}. The main task of <em>this</em> class is to provide the rule that,
 * on the token parsed and verified by the {@link TokenValidationService}, determines whether it's a VP or a VC and
 * validates its contents.
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


    /**
     * Determines whether the input is a JWT that is either a credential or a VCDM 1.1 presentation. Only the format is
     * checked, the signature is verified by {@link #verify(String, VerifierContext)}.
     */
    @Override
    public boolean canHandle(String rawInput) {
        return tokenValidationService.validate(rawInput, TokenValidationService.NO_SIGNATURE_VERIFICATION, List.of(isCredentialOrVcdm1_1presentation()))
                .succeeded();
    }

    /**
     * Verifies the presentation by checking the cryptographic integrity as well as the presence of mandatory claims in the JWT.
     * The JWT is parsed and its signature verified once by the {@link TokenValidationService}, then a single
     * {@link TokenValidationRule} determines whether it is a credential or a presentation and validates it accordingly.
     *
     * @param serializedJwt The serialized JWT presentation to be verified.
     * @return A Result object representing the verification result, containing specific error messages in case of failure.
     */
    @Override
    public Result<Void> verify(String serializedJwt, VerifierContext context) {
        return tokenValidationService.validate(serializedJwt, publicKeyResolver, List.of(verifiableDocumentRule(context)))
                .mapEmpty();
    }

    /**
     * Returns the rule that accepts the token if it is a credential or a VCDM 1.1 presentation.
     */
    private TokenValidationRule isCredentialOrVcdm1_1presentation() {
        return (claimToken, additional) -> {
            var claims = claimToken.getClaims();
            return isCredential(claims) || JwtPresentationFormat.of(claims) == CredentialFormat.VC1_0_JWT
                    ? Result.success()
                    : Result.failure("Neither a credential nor a VCDM 1.1 presentation");
        };
    }

    /**
     * Returns the rule that validates the token as either a credential or a presentation, depending on its claims.
     */
    private TokenValidationRule verifiableDocumentRule(VerifierContext context) {
        return (claimToken, additional) -> {
            var claims = claimToken.getClaims();

            if (isCredential(claims)) {
                var rules = tokenValidationRulesRegistry.getRules(Constants.JWT_VC_TOKEN_CONTEXT);
                return TokenValidationRule.allOf(rules).checkRule(claimToken, additional);
            }

            if (context.isCredentialExpected()) {
                return Result.failure("Expected a credential, but the JWT contains a presentation.");
            }

            if (isPresentation(claims)) {
                return validateAsPresentation(claimToken, additional, context);
            }

            return Result.failure("Either '%s' or '%s' claim must be present in JWT.".formatted(Constants.VP_CLAIM, Constants.VC_CLAIM));
        };
    }

    private Result<Void> validateAsPresentation(ClaimToken claimToken, @Nullable Map<String, Object> additional, VerifierContext context) {
        var verificationResult = TokenValidationRule.allOf(vpValidationRules(context.getAudience())).checkRule(claimToken, additional);
        if (verificationResult.failed()) {
            // the credentials of a presentation that is not valid are not verified
            return verificationResult;
        }

        var vpClaim = vpClaim(claimToken.getClaim(Constants.VP_CLAIM));
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

        // every VC is represented as another JWT or as a JSON-LD document, each one is verified (and its signature checked) on its own
        var credentialContext = context.toBuilder()
                .audience(claimToken.getStringClaim(JwtRegisteredClaimNames.ISSUER))
                .credentialExpected(true)
                .build();

        return rawCredentials.stream().map(credentialContext::verify).reduce(Result.success(), Result::merge);
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
            } catch (JacksonException e) {
                return null;
            }
        }
        return vpClaim instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private List<TokenValidationRule> vpValidationRules(String audience) {
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(Constants.JWT_VP_TOKEN_CONTEXT));
        rules.add(new IssuerKeyIdValidationRule());
        rules.add(new PresentationFormatRule(CredentialFormat.VC1_0_JWT));
        if (audience != null) {
            rules.add(new AudienceValidationRule(audience));
        }

        return rules;
    }

    private boolean isCredential(Map<String, Object> claims) {
        return claims.containsKey(Constants.VC_CLAIM) && !claims.containsKey(Constants.VP_CLAIM);
    }

    private boolean isPresentation(Map<String, Object> claims) {
        return claims.get(Constants.VP_CLAIM) != null;
    }

    private List<String> extractCredentials(Object credentialsObject) {
        if (credentialsObject instanceof Collection<?> credentialsCollection) {
            return credentialsCollection.stream()
                    .map(obj -> (obj instanceof String stringCredential)
                        ? stringCredential
                        : typeManager.getMapper(typeContext).writeValueAsString(obj))
                    .toList();
        }
        return List.of(credentialsObject.toString());
    }
}
