/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Think-it GmbH - presentation format detection
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt;

import com.nimbusds.jose.HeaderParameterNames;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.JwtPresentationFormat;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.CredentialIssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.EnvelopedCredentialsRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.EnvelopedPresentationsRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.NotPresentationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.PresentationFormatRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.PresentationHolderKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.PresentationTypeRule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VC_CLAIM;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_PRESENTATION_TYPE;

/**
 * This verifier verifies a JWT that contains a Verifiable Presentation (VP) as JSON payload that conforms to the
 * Verifiable Credentials Data Model (VCDM) 2.0 specification. Both enveloped presentations and simple VP tokens are supported.
 * Every presentation JWT, and the surrounding envelope, must be signed with a key of the presentation's {@code holder}.
 * The JWT must be a VCDM 2.0 presentation as determined by {@link JwtPresentationFormat}.
 */
public class Vcdm20JosePresentationVerifier implements CredentialVerifier {
    private static final String VP_JWT_TYPE = "vp+jwt";

    private final Monitor monitor;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRulesRegistry;
    private final PublicKeyResolver publicKeyResolver;
    private final boolean requireAudience;

    /**
     * Creates a verifier that checks the presentation JWTs with the rules registered for {@link Constants#JWT_VP_TOKEN_CONTEXT}.
     * Presentation JWTs without a {@code typ} header are accepted, but a warning is logged.
     *
     * @param requireAudience whether the presentation JWTs must have an {@code aud} claim. Otherwise, it is only checked when present.
     */
    public Vcdm20JosePresentationVerifier(Monitor monitor, TokenValidationService tokenValidationService, TokenValidationRulesRegistry tokenValidationRulesRegistry,
                                          PublicKeyResolver publicKeyResolver, boolean requireAudience) {
        this.monitor = monitor;
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRulesRegistry = tokenValidationRulesRegistry;
        this.publicKeyResolver = publicKeyResolver;
        this.requireAudience = requireAudience;
    }

    /**
     * Determines whether the input is a JWT that contains a VCDM 2.0 presentation. Only the format is checked, the
     * signature is verified by {@link #verify(String, VerifierContext)}.
     */
    @Override
    public boolean canHandle(String rawInput) {
        var rules = List.of(isVcdm2_0presentation());
        return tokenValidationService.validate(rawInput, TokenValidationService.NO_SIGNATURE_VERIFICATION, rules).succeeded();
    }

    /**
     * Verifies the presentation. The JWT is parsed and its signature verified once by the {@link TokenValidationService},
     * then a single {@link TokenValidationRule} validates it as an enveloped presentation or as a VP token.
     */
    @Override
    public Result<Void> verify(String rawInput, VerifierContext verifierContext) {
        if (verifierContext.isCredentialExpected()) {
            return Result.failure("Expected a credential, but the JWT contains a presentation.");
        }

        var rules = List.of(presentationRule(verifierContext.getAudience()));
        return tokenValidationService.validate(rawInput, publicKeyResolver, rules).mapEmpty();
    }

    /**
     * Returns the rule that accepts the token if it is a VCDM 2.0 presentation, either enveloped or not.
     */
    private TokenValidationRule isVcdm2_0presentation() {
        return (claimToken, additional) -> {
            var claims = claimToken.getClaims();
            return JwtPresentationFormat.of(claims) == CredentialFormat.VC2_0_JOSE && presentationType(claims) != null
                    ? Result.success()
                    : Result.failure("Not a VCDM 2.0 presentation");
        };
    }

    /**
     * Returns the rule that validates the token as an enveloped presentation or as a VP token, depending on its type.
     */
    private TokenValidationRule presentationRule(String audience) {
        return (claimToken, additional) -> {
            warnIfUntyped(claimToken);

            if (ENVELOPED_VERIFIABLE_PRESENTATION_TYPE.equals(presentationType(claimToken.getClaims()))) {
                var rules = vpValidationRules(audience);
                rules.add(new EnvelopedPresentationsRule((presentationJwt, envelope) -> tokenValidationService.validate(presentationJwt,
                        publicKeyResolver, List.of(envelopedPresentationRule(audience, envelope.getKeyId()))).mapEmpty()));
                return TokenValidationRule.allOf(rules).checkRule(claimToken, additional);
            }

            // any other JWT must be a presentation, which the rules assert
            return TokenValidationRule.allOf(presentationValidationRules(audience, claimToken.getKeyId()))
                    .checkRule(claimToken, additional);
        };
    }

    /**
     * Returns the rule that validates a presentation contained in an envelope: it must be signed with a key of its
     * holder, as well as the envelope.
     */
    private TokenValidationRule envelopedPresentationRule(@Nullable String audience, @Nullable String envelopeKeyId) {
        return (claimToken, additional) -> {
            warnIfUntyped(claimToken);

            var rules = presentationValidationRules(audience, claimToken.getKeyId());
            rules.add(new PresentationHolderKeyIdValidationRule(envelopeKeyId));
            return TokenValidationRule.allOf(rules).checkRule(claimToken, additional);
        };
    }

    /**
     * Returns the rule that validates a credential: VCDM 1.1 credentials (with a "vc" claim) with the same rules as in a
     * VCDM 1.1 presentation, any other credential must be signed with a key of its issuer.
     */
    private TokenValidationRule credentialRule() {
        return (claimToken, additional) -> {
            List<TokenValidationRule> rules = claimToken.getClaims().containsKey(VC_CLAIM)
                    ? tokenValidationRulesRegistry.getRules(Constants.JWT_VC_TOKEN_CONTEXT)
                    : List.of(new CredentialIssuerKeyIdValidationRule(claimToken.getKeyId()));
            return TokenValidationRule.allOf(rules).checkRule(claimToken, additional);
        };
    }

    /**
     * Returns the presentation type of the JWT. The {@code type} claim is either a single string or a list that contains the type.
     *
     * @return {@link Constants#ENVELOPED_VERIFIABLE_PRESENTATION_TYPE}, {@link Constants#VERIFIABLE_PRESENTATION_TYPE}, or null if it is neither
     */
    private @Nullable String presentationType(Map<String, Object> claims) {
        var type = claims.get(TYPE);
        var types = type instanceof List<?> list ? list : Collections.singletonList(type);
        return Stream.of(ENVELOPED_VERIFIABLE_PRESENTATION_TYPE, VERIFIABLE_PRESENTATION_TYPE)
                .filter(types::contains)
                .findFirst()
                .orElse(null);
    }

    /**
     * VC-JOSE-COSE expects presentation JWTs to carry the {@code typ} header {@code vp+jwt}, but presentations without it are still accepted.
     */
    private void warnIfUntyped(ClaimToken presentation) {
        if (presentation.getHeader(HeaderParameterNames.TYPE) == null) {
            monitor.warning("Presentation JWT has no 'typ' header, expected '%s'. It is accepted nonetheless.".formatted(VP_JWT_TYPE));
        }
    }

    /**
     * Returns the rules for a JWT that carries a presentation: it must be a VCDM 2.0 presentation of type
     * {@link Constants#VERIFIABLE_PRESENTATION_TYPE}, signed with a key of the presentation's holder.
     */
    private List<TokenValidationRule> presentationValidationRules(@Nullable String audience, @Nullable String keyId) {
        var rules = vpValidationRules(audience);
        rules.add(new PresentationTypeRule(VERIFIABLE_PRESENTATION_TYPE));
        rules.add(new PresentationHolderKeyIdValidationRule(keyId));
        rules.add(new EnvelopedCredentialsRule(credentialJwt -> tokenValidationService.validate(credentialJwt,
                publicKeyResolver, List.of(credentialRule(), new NotPresentationRule())).mapEmpty()));
        return rules;
    }

    private List<TokenValidationRule> vpValidationRules(@Nullable String audience) {
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(Constants.JWT_VP_TOKEN_CONTEXT));
        rules.add(new PresentationFormatRule(CredentialFormat.VC2_0_JOSE));
        if (audience != null) {
            rules.add(new AudienceValidationRule(audience, !requireAudience));
        }
        return rules;
    }
}
