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

package org.eclipse.edc.iam.identitytrust.verification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Computes the cryptographic integrity of a VerifiablePresentation when it's represented as JWT.
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
 *     </li>
 * </ul>
 *
 * <em>Note: VP-JWTs may only contain VCs also represented in JWT format. Mixing formats is not allowed.</em>
 */
class JwtPresentationVerifier {
    public static final String VERIFIABLE_CREDENTIAL_JSON_KEY = "verifiableCredential";
    public static final String VP_CLAIM = "vp";
    private final JwtVerifier jwtVerifier;
    private final String thisAudience;
    private final ObjectMapper objectMapper;

    /**
     * Verifies the JWT presentation by checking the cryptographic integrity.
     *
     * @param jwtVerifier  The JwtVerifier instance used to verify the JWT token.
     * @param thisAudience The audience to which the token is intended. This must be "this connector's identifier"
     */
    JwtPresentationVerifier(JwtVerifier jwtVerifier, String thisAudience, ObjectMapper objectMapper) {
        this.jwtVerifier = jwtVerifier;
        this.thisAudience = thisAudience;
        this.objectMapper = objectMapper;
    }


    /**
     * Verifies the presentation by checking the cryptographic integrity as well as the presence of mandatory claims in the JWT.
     *
     * @param serializedJwt The serialized JWT presentation to be verified.
     * @return A Result object representing the verification result, containing specific error messages in case of failure.
     */
    public Result<Void> verifyPresentation(String serializedJwt) {

        // verify the "outer" JWT, i.e. the VP JWT
        var vpResult = jwtVerifier.verify(serializedJwt, thisAudience);
        if (vpResult.failed()) {
            return vpResult;
        }

        // verify all "inner" VC JWTs
        try {
            // obtain the actual VP JSON structure
            var signedVpJwt = SignedJWT.parse(serializedJwt);
            var vpClaim = signedVpJwt.getJWTClaimsSet().getClaim(VP_CLAIM);
            if (vpClaim == null) {
                return Result.failure("VP-JWT does not contain mandatory claim: " + VP_CLAIM);
            }
            var vpJson = vpClaim.toString();

            // obtain the "verifiableCredentials" object inside
            var map = objectMapper.readValue(vpJson, Map.class);
            if (!map.containsKey(VERIFIABLE_CREDENTIAL_JSON_KEY)) {
                return Result.failure("Presentation object did not contain mandatory object: " + VERIFIABLE_CREDENTIAL_JSON_KEY);
            }
            var vcTokens = extractCredentials(map.get(VERIFIABLE_CREDENTIAL_JSON_KEY));

            if (vcTokens.isEmpty()) {
                // todo: this is allowed by the spec, but it is semantic nonsense. Should we return failure or not?
                return Result.failure("No credential found in presentation.");
            }

            // every VC is represented as another JWT, so we verify all of them
            for (String token : vcTokens) {
                vpResult = vpResult.merge(jwtVerifier.verify(token, signedVpJwt.getJWTClaimsSet().getIssuer()));
            }

        } catch (ParseException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return vpResult;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractCredentials(Object credentialsObject) {
        if (credentialsObject instanceof Collection<?>) {
            return ((Collection) credentialsObject).stream().map(Object::toString).toList();
        }
        return List.of(credentialsObject.toString());
    }
}
