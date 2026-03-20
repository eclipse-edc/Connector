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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_CREDENTIAL_CONTENT_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_CREDENTIAL_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_PRESENTATION_CONTENT_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ID;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_CREDENTIAL_JSON_KEY;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VP_CLAIM;

/**
 * This verifier verifies a JWT that contains a Verifiable Presentation (VP) as JSON payload that conforms to the
 * Verifiable Credentials Data Model (VCDM) 2.0 specification. Both enveloped presentations and simple VP tokens are supported.
 */
public class Vcdm20JosePresentationVerifier implements CredentialVerifier {
    private final TokenValidationService tokenValidationService;
    private final PublicKeyResolver publicKeyResolver;

    public Vcdm20JosePresentationVerifier(TokenValidationService tokenValidationService, PublicKeyResolver publicKeyResolver) {
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
    }

    @Override
    public boolean canHandle(String rawInput) {
        try {
            var signedJwt = SignedJWT.parse(rawInput);
            var jwtClaimsSet = signedJwt.getJWTClaimsSet();
            if (jwtClaimsSet.getClaims().containsKey(VP_CLAIM)) { // VCDM1.1
                return false;
            }

            var type = jwtClaimsSet.getStringClaim(TYPE);
            if (type == null) {
                return false;
            }

            return type.equals(ENVELOPED_VERIFIABLE_PRESENTATION_TYPE) || type.equals(VERIFIABLE_PRESENTATION_TYPE);

        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public Result<Void> verify(String rawInput, VerifierContext verifierContext) {
        try {
            var jwtClaimsSet = SignedJWT.parse(rawInput).getJWTClaimsSet();
            var type = jwtClaimsSet.getStringClaim(TYPE);
            if (type == null) {
                return Result.failure("Not a valid VP token - missing the 'type' claim");
            }

            // validate JWT signature
            var verificationResult = tokenValidationService.validate(rawInput, publicKeyResolver);
            if (verificationResult.failed()) {
                return verificationResult.mapEmpty();
            }

            return switch (type) {
                case ENVELOPED_VERIFIABLE_PRESENTATION_TYPE -> verifyEnvelopedPresentation(jwtClaimsSet);
                case VERIFIABLE_PRESENTATION_TYPE -> verifyVpToken(jwtClaimsSet);
                default -> Result.failure("Not a valid VP token - unknown type '%s'".formatted(type));
            };


        } catch (ParseException e) {
            return Result.failure("Error parsing JWT: " + e.getMessage());
        }
    }


    /**
     * This method verifies an "Enveloped Presentation", which is a compact form of enclosing a VPs in a JWT. The 'id'
     * claim contains a semicolon-separated list of JWTs, prefixed with "data:application/vp+jwt,", where each contains
     * a single Verifiable Presentation. Each of these JWTs represents a "Credential Envelope".
     *
     * @param claimsSet the parsed JWT claims representing the enveloped VP
     * @return a successful result only if all presentations and all JWTs in them are valid (i.e. their signatures are correct).
     * @throws ParseException if parsing one of the JWTs fails
     */
    private Result<Void> verifyEnvelopedPresentation(JWTClaimsSet claimsSet) throws ParseException {
        var vpEnvelopeString = claimsSet.getStringClaim(ID);
        if (vpEnvelopeString == null) {
            return Result.failure("No enveloped credential found in 'id' claim");
        }

        var vpEnvelopes = vpEnvelopeString.split(";");

        var results = new ArrayList<Result<Void>>();
        for (var vpEnvelope : vpEnvelopes) {
            if (!vpEnvelope.startsWith(ENVELOPED_PRESENTATION_CONTENT_TYPE + ",")) {
                return Result.failure("Incorrect presentation envelope. Must start with '%s,' but it did not.".formatted(ENVELOPED_PRESENTATION_CONTENT_TYPE));
            }
            vpEnvelope = vpEnvelope.substring(ENVELOPED_PRESENTATION_CONTENT_TYPE.length() + 1);
            results.add(tokenValidationService.validate(vpEnvelope, publicKeyResolver).mapEmpty());
            results.add(verifyVpToken(SignedJWT.parse(vpEnvelope).getJWTClaimsSet()));
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
                return Result.failure("No verifiable credential found in 'verifiableCredential' claim");
            }

            var results = new ArrayList<Result<Void>>();
            for (var credential : credentialObjectList) {
                //noinspection unchecked
                var credentialObject = (Map<String, Object>) credential;
                var credentialEnvelopeType = credentialObject.get(TYPE);
                if (!ENVELOPED_CREDENTIAL_TYPE.equals(credentialEnvelopeType)) {
                    return Result.failure("Incorrect 'type' field in verifiable credential. Must be '%s' but was '%s'".formatted(ENVELOPED_CREDENTIAL_TYPE, credentialEnvelopeType));
                }
                // expect enveloped credentials here. there can be multiple, separated by ";"
                var credentialEnvelope = credentialObject.get(ID);
                if (credentialEnvelope instanceof String credentialEnvelopeString) {
                    results.add(verifyCredentialEnvelope(credentialEnvelopeString));
                } else {
                    return Result.failure("No enveloped credential found in 'id' claim");
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
     * Note that these JWTs do not contain any registered claim names, so we can only validate their signatures.
     *
     * @param credentialEnvelopeString A semicolon separated list of JWTS, prefixed with "data:application/vc+jwt,", for example:
     *                                 <pre>data:application/vc+jwt:,eyAB....,data:application/vc+jwt,eyXYZ...</pre>
     * @return a successful result only if all JWTs are valid (i.e. their signatures are correct).
     */
    private Result<Void> verifyCredentialEnvelope(String credentialEnvelopeString)  {
        // a credential envelope can contain multiple credentials, prefixed with "data:application/vc+jwt," and separated by ";"

        var credentialEnvelopes = credentialEnvelopeString.split(";");

        var results = new ArrayList<Result<Void>>();
        for (var envelope : credentialEnvelopes) {
            if (!envelope.startsWith(ENVELOPED_CREDENTIAL_CONTENT_TYPE + ",")) {
                return Result.failure("Incorrect credential envelope. Must start with '%s,' but it did not.".formatted(ENVELOPED_CREDENTIAL_CONTENT_TYPE));
            }
            // we don't need to parse the JWT into a VC, because VC validation happens elsewhere
            envelope = envelope.substring(ENVELOPED_CREDENTIAL_CONTENT_TYPE.length() + 1);
            results.add(tokenValidationService.validate(envelope, publicKeyResolver).mapEmpty());
        }

        return results.stream().reduce(Result::merge).orElse(Result.success());
    }
}
