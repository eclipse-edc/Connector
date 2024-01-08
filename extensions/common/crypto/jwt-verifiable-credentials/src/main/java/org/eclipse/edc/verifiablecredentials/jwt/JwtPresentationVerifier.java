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
import org.eclipse.edc.identitytrust.validation.TokenValidationAction;
import org.eclipse.edc.identitytrust.verification.CredentialVerifier;
import org.eclipse.edc.identitytrust.verification.VerifierContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Computes the cryptographic integrity of a VerifiablePresentation when it's represented as JWT. Internally, for the actual
 * cryptographic computation it uses the generic {@link JwtVerifier} object. The main task of <em>this</em> class is to read the JWT,
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
    public static final String VERIFIABLE_CREDENTIAL_JSON_KEY = "verifiableCredential";
    public static final String VP_CLAIM = "vp";
    public static final String VC_CLAIM = "vc";
    private final TokenValidationAction jwtValidationAction;
    private final ObjectMapper objectMapper;

    /**
     * Verifies the JWT presentation by checking the cryptographic integrity.
     *
     * @param validationAction a {@link TokenValidationAction} that is executed to verify and validate the token.
     */
    public JwtPresentationVerifier(TokenValidationAction validationAction, ObjectMapper objectMapper) {
        this.jwtValidationAction = validationAction;
        this.objectMapper = objectMapper;
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
        var audience = context.getAudience();
        var verificationResult = jwtValidationAction.apply(TokenRepresentation.Builder.newInstance().token(serializedJwt).build());
        if (verificationResult.failed()) {
            return verificationResult.mapTo();
        }

        // verify all "inner" VC JWTs
        try {
            // obtain the actual JSON structure
            var signedJwt = SignedJWT.parse(serializedJwt);
            if (isCredential(signedJwt)) {
                return verificationResult.mapTo();
            }

            if (!isPresentation(signedJwt)) {
                return Result.failure("Either '%s' or '%s' claim must be present in JWT.".formatted(VP_CLAIM, VC_CLAIM));
            }

            var vpClaim = signedJwt.getJWTClaimsSet().getClaim(VP_CLAIM);
            var vpJson = vpClaim.toString();

            // obtain the "verifiableCredentials" object inside
            var map = objectMapper.readValue(vpJson, Map.class);
            if (!map.containsKey(VERIFIABLE_CREDENTIAL_JSON_KEY)) {
                return Result.failure("Presentation object did not contain mandatory object: " + VERIFIABLE_CREDENTIAL_JSON_KEY);
            }
            var rawCredentials = extractCredentials(map.get(VERIFIABLE_CREDENTIAL_JSON_KEY));

            if (rawCredentials.isEmpty()) {
                // todo: this is allowed by the spec, but it is semantic nonsense. Should we return failure or not?
                return Result.success();
            }

            // every VC is represented as another JWT, so we verify all of them
            for (String token : rawCredentials) {
                verificationResult = verificationResult.merge(context.withAudience(signedJwt.getJWTClaimsSet().getIssuer()).verify(token));
            }

        } catch (ParseException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return verificationResult.mapTo();
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
