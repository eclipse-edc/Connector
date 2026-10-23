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
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.CredentialIssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.PresentationHolderKeyIdValidationRule;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_CREDENTIAL_CONTENT_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_CREDENTIAL_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_PRESENTATION_CONTENT_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ID;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VC_CLAIM;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_CREDENTIAL_JSON_KEY;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VP_CLAIM;

/**
 * This verifier verifies a JWT that contains a Verifiable Presentation (VP) as JSON payload that conforms to the
 * Verifiable Credentials Data Model (VCDM) 2.0 specification. Both enveloped presentations and simple VP tokens are supported.
 * Every presentation JWT, and the envelope around it, must be signed with a key of the presentation's {@code holder}.
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

    @Override
    public boolean canHandle(String rawInput) {
        try {
            var jwtClaimsSet = SignedJWT.parse(rawInput).getJWTClaimsSet();
            if (jwtClaimsSet.getClaims().containsKey(VP_CLAIM)) { // VCDM1.1
                return false;
            }

            return presentationType(jwtClaimsSet) != null;

        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public Result<Void> verify(String rawInput, VerifierContext verifierContext) {
        try {
            var jwt = SignedJWT.parse(rawInput);
            var jwtClaimsSet = jwt.getJWTClaimsSet();
            if (jwtClaimsSet.getClaim(TYPE) == null) {
                return Result.failure("Not a valid VP token - missing the 'type' claim");
            }
            var type = presentationType(jwtClaimsSet);
            if (type == null) {
                return Result.failure("Not a valid VP token - unknown type '%s'".formatted(jwtClaimsSet.getClaim(TYPE)));
            }

            warnIfUntyped(jwt);

            var keyId = jwt.getHeader().getKeyID();
            if (ENVELOPED_VERIFIABLE_PRESENTATION_TYPE.equals(type)) {
                // the envelope has no holder, its key is checked against the holder of each presentation it contains
                var verificationResult = tokenValidationService.validate(rawInput, publicKeyResolver, vpValidationRules(verifierContext.getAudience()));
                if (verificationResult.failed()) {
                    return verificationResult.mapEmpty();
                }
                return verifyEnvelopedPresentation(jwtClaimsSet, keyId, verifierContext.getAudience());
            }

            var verificationResult = tokenValidationService.validate(rawInput, publicKeyResolver, vpValidationRules(verifierContext.getAudience(), keyId));
            if (verificationResult.failed()) {
                return verificationResult.mapEmpty();
            }
            return verifyVpToken(jwtClaimsSet);


        } catch (ParseException e) {
            return Result.failure("Error parsing JWT: " + e.getMessage());
        }
    }


    /**
     * This method verifies an "Enveloped Presentation", which is a compact form of enclosing a VPs in a JWT. The 'id'
     * claim contains a semicolon-separated list of JWTs, prefixed with "data:application/vp+jwt,", where each contains
     * a single Verifiable Presentation. Each of these JWTs represents a "Credential Envelope".
     *
     * @param claimsSet      the parsed JWT claims representing the enveloped VP
     * @param envelopeKeyId  the key ID that signed the envelope, which must belong to the holder of every presentation in it
     * @param audience       the expected audience of the presentation JWTs, if any
     * @return a successful result only if all presentations and all JWTs in them are valid (i.e. their signatures are correct
     *         and they are signed by the presentation's holder).
     * @throws ParseException if parsing one of the JWTs fails
     */
    private Result<Void> verifyEnvelopedPresentation(JWTClaimsSet claimsSet, @Nullable String envelopeKeyId, @Nullable String audience) throws ParseException {
        var vpEnvelopeString = claimsSet.getStringClaim(ID);
        if (vpEnvelopeString == null) {
            return Result.failure("No enveloped credential found in '%s' claim".formatted(ID));
        }

        var vpEnvelopes = vpEnvelopeString.split(";");

        var results = new ArrayList<Result<Void>>();
        for (var vpEnvelope : vpEnvelopes) {
            if (!vpEnvelope.startsWith(ENVELOPED_PRESENTATION_CONTENT_TYPE + ",")) {
                return Result.failure("Incorrect presentation envelope. Must start with '%s,' but it did not.".formatted(ENVELOPED_PRESENTATION_CONTENT_TYPE));
            }
            vpEnvelope = vpEnvelope.substring(ENVELOPED_PRESENTATION_CONTENT_TYPE.length() + 1);
            var vpJwt = SignedJWT.parse(vpEnvelope);
            var rules = vpValidationRules(audience, vpJwt.getHeader().getKeyID());
            rules.add(new PresentationHolderKeyIdValidationRule(envelopeKeyId));
            results.add(tokenValidationService.validate(vpEnvelope, publicKeyResolver, rules).mapEmpty());
            warnIfUntyped(vpJwt);
            results.add(verifyVpToken(vpJwt.getJWTClaimsSet()));
        }
        return results.stream().reduce(Result::merge).orElse(Result.success());
    }


    /**
     * This method validates a single "Presentation Envelope", which is a JWT token that carries a verifiable presentation as JSON payload.
     *
     * @param claimsSet the parsed JWT claims representing the enveloped VP
     * @return a successful result only if the JWT is valid (i.e. its signature is correct).
     */
    private Result<Void> verifyVpToken(JWTClaimsSet claimsSet) {
        try {
            var credentialObjectList = claimsSet.getListClaim(VERIFIABLE_CREDENTIAL_JSON_KEY);
            if (credentialObjectList == null) {
                return Result.failure("No verifiable credential found in '%s' claim".formatted(VERIFIABLE_CREDENTIAL_JSON_KEY));
            }

            var results = new ArrayList<Result<Void>>();
            for (var credential : credentialObjectList) {
                //noinspection unchecked
                var credentialObject = (Map<String, Object>) credential;
                var credentialEnvelopeType = credentialObject.get(TYPE);
                if (!ENVELOPED_CREDENTIAL_TYPE.equals(credentialEnvelopeType)) {
                    return Result.failure("Incorrect '%s' field in verifiable credential. Must be '%s' but was '%s'".formatted(TYPE, ENVELOPED_CREDENTIAL_TYPE, credentialEnvelopeType));
                }
                // expect enveloped credentials here. there can be multiple, separated by ";"
                var credentialEnvelope = credentialObject.get(ID);
                if (credentialEnvelope instanceof String credentialEnvelopeString) {
                    results.add(verifyCredentialEnvelope(credentialEnvelopeString));
                } else {
                    return Result.failure("No enveloped credential found in '%s' claim".formatted(ID));
                }
            }
            return results.stream().reduce(Result::merge).orElse(Result.success());

        } catch (ParseException e) {
            return Result.failure("Error parsing JSON: " + e.getMessage());
        }
    }


    /**
     * This method verifies a "Credential Envelope", which is a semicolon separated list of JWTS, prefixed with "data:application/vc+jwt,", where
     * each contains a single Verifiable Credential as JSON payload. N
     * Besides the signature, the key that signed a JWT must belong to the credential's issuer. VCDM 1.1 credentials
     * (i.e. JWTs with a "vc" claim) are validated with the same rules as in a VCDM 1.1 presentation.
     *
     * @param credentialEnvelopeString A semicolon separated list of JWTS, prefixed with "data:application/vc+jwt,", for example:
     *                                 <pre>data:application/vc+jwt:,eyAB....,data:application/vc+jwt,eyXYZ...</pre>
     * @return a successful result only if all JWTs are valid (i.e. their signatures are correct and they are signed by their issuer).
     * @throws ParseException if parsing one of the JWTs fails
     */
    private Result<Void> verifyCredentialEnvelope(String credentialEnvelopeString) throws ParseException {
        // a credential envelope can contain multiple credentials, prefixed with "data:application/vc+jwt," and separated by ";"

        var credentialEnvelopes = credentialEnvelopeString.split(";");

        var results = new ArrayList<Result<Void>>();
        for (var envelope : credentialEnvelopes) {
            if (!envelope.startsWith(ENVELOPED_CREDENTIAL_CONTENT_TYPE + ",")) {
                return Result.failure("Incorrect credential envelope. Must start with '%s,' but it did not.".formatted(ENVELOPED_CREDENTIAL_CONTENT_TYPE));
            }
            // we don't need to parse the JWT into a VC, because VC validation happens elsewhere
            envelope = envelope.substring(ENVELOPED_CREDENTIAL_CONTENT_TYPE.length() + 1);
            results.add(tokenValidationService.validate(envelope, publicKeyResolver, credentialValidationRules(SignedJWT.parse(envelope))).mapEmpty());
        }

        return results.stream().reduce(Result::merge).orElse(Result.success());
    }

    private List<TokenValidationRule> credentialValidationRules(SignedJWT credential) throws ParseException {
        var keyId = credential.getHeader().getKeyID();
        if (credential.getJWTClaimsSet().getClaims().containsKey(VC_CLAIM)) {
            var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(Constants.JWT_VC_TOKEN_CONTEXT));
            rules.add(new IssuerKeyIdValidationRule(keyId));
            return rules;
        }
        return List.of(new CredentialIssuerKeyIdValidationRule(keyId));
    }

    /**
     * Returns the presentation type of the JWT. The {@code type} claim is either a single string or a list that contains the type.
     *
     * @return {@link Constants#ENVELOPED_VERIFIABLE_PRESENTATION_TYPE}, {@link Constants#VERIFIABLE_PRESENTATION_TYPE}, or null if it is neither
     */
    private @Nullable String presentationType(JWTClaimsSet claimsSet) {
        var type = claimsSet.getClaim(TYPE);
        var types = type instanceof List<?> list ? list : Collections.singletonList(type);
        return Stream.of(ENVELOPED_VERIFIABLE_PRESENTATION_TYPE, VERIFIABLE_PRESENTATION_TYPE)
                .filter(types::contains)
                .findFirst()
                .orElse(null);
    }

    /**
     * VC-JOSE-COSE expects presentation JWTs to carry the {@code typ} header {@code vp+jwt}, but presentations without it are still accepted.
     */
    private void warnIfUntyped(SignedJWT presentation) {
        if (presentation.getHeader().getType() == null) {
            monitor.warning("Presentation JWT has no 'typ' header, expected '%s'. It is accepted nonetheless.".formatted(VP_JWT_TYPE));
        }
    }

    private List<TokenValidationRule> vpValidationRules(@Nullable String audience) {
        var rules = new ArrayList<>(tokenValidationRulesRegistry.getRules(Constants.JWT_VP_TOKEN_CONTEXT));
        if (audience != null) {
            rules.add(new AudienceValidationRule(audience, !requireAudience));
        }
        return rules;
    }

    /**
     * Returns the rules for a JWT that carries a presentation, which must be signed with a key of the presentation's holder.
     */
    private List<TokenValidationRule> vpValidationRules(@Nullable String audience, @Nullable String keyId) {
        var rules = vpValidationRules(audience);
        rules.add(new PresentationHolderKeyIdValidationRule(keyId));
        return rules;
    }
}
