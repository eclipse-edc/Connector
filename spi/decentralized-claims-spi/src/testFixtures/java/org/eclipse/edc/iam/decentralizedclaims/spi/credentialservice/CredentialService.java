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

package org.eclipse.edc.iam.decentralizedclaims.spi.credentialservice;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test fixture that simulates a multi-participant Credential Service with resolvable {@code did:web}
 * DIDs and mock presentation query endpoints.
 * <p>
 * Each participant context has its own EC key pair, DID, DID document endpoint, credential storage,
 * and presentation query endpoint. Participants are registered via {@link #addParticipant(String)}.
 */
public class CredentialService {

    private final Map<String, ParticipantContext> participants = new HashMap<>();
    private final Integer port;

    public CredentialService(Integer port) {
        this.port = port;
    }

    private static JWK generateKey(String participantContextId, String did) {
        try {
            return new ECKeyGenerator(Curve.P_256)
                    .keyID(did + "#key1")
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate EC key for participant: " + participantContextId, e);
        }
    }

    /**
     * Registers a new participant context. Generates an EC key pair, creates a DID document,
     * and stubs the DID resolution and presentation query endpoints for this participant.
     *
     * @param participantContextId the participant context identifier
     */
    public void addParticipant(String participantContextId, JWK additionalKey) {
        if (participants.containsKey(participantContextId)) {
            throw new IllegalArgumentException("Participant already registered: " + participantContextId);
        }

        var did = didFor(participantContextId);
        var credentialServiceUrl = "http://localhost:" + port + "/credentials/" + participantContextId;

        var ecKey = generateKey(participantContextId, did);

        var verificationMethods = new ArrayList<VerificationMethod>();
        verificationMethods.add(VerificationMethod.Builder.newInstance()
                .id(ecKey.getKeyID())
                .type("JsonWebKey2020")
                .controller(did)
                .publicKeyJwk(ecKey.toPublicJWK().toJSONObject())
                .build());

        if (additionalKey != null) {
            verificationMethods.add(VerificationMethod.Builder.newInstance()
                    .id(additionalKey.getKeyID())
                    .type("JsonWebKey2020")
                    .controller(did)
                    .publicKeyJwk(additionalKey.toPublicJWK().toJSONObject())
                    .build());
        }


        var didDocument = DidDocument.Builder.newInstance()
                .id(did)
                .verificationMethod(
                        verificationMethods
                )
                .service(List.of(new Service(
                        UUID.randomUUID().toString(),
                        "CredentialService",
                        credentialServiceUrl)))
                .build();

        var ctx = new ParticipantContext(didDocument, ecKey);
        participants.put(participantContextId, ctx);
    }

    /**
     * Registers a new participant context. Generates an EC key pair, creates a DID document,
     * and stubs the DID resolution and presentation query endpoints for this participant.
     *
     * @param participantContextId the participant context identifier
     */
    public void addParticipant(String participantContextId) {
        addParticipant(participantContextId, null);
    }

    public @NonNull String didFor(String participantContextId) {
        return "did:web:localhost%3A" + port + ":" + participantContextId;
    }

    public DidDocument getDidDocument(String participantContextId) {
        return getParticipant(participantContextId).document;
    }

    public String createStsToken(String participantContextId, String audience, String bearerAccessScope, String bearerAccessToken) {
        var ctx = getParticipant(participantContextId);
        return createStsToken(ctx, audience, bearerAccessScope, bearerAccessToken);
    }

    /**
     * Stores a verifiable credential JWT for the given participant and updates the presentation endpoint response.
     *
     * @param participantContextId the participant context identifier
     * @param vcJwt                the serialized JWT string of the verifiable credential
     */
    public void storeCredential(String participantContextId, String vcJwt) {
        var ctx = getParticipant(participantContextId);
        ctx.storedCredentials.add(vcJwt);
    }

    private ParticipantContext getParticipant(String participantContextId) {
        var ctx = participants.get(participantContextId);
        if (ctx == null) {
            throw new IllegalArgumentException("Unknown participant: " + participantContextId);
        }
        return ctx;
    }

    private String createStsToken(ParticipantContext ctx, String audience, String bearerAccessScope, String bearerAccessToken) {
        try {
            var now = Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(ctx.document.getId())
                    .subject(ctx.document.getId())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(now)
                    .notBeforeTime(now)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .audience(audience);

            if (bearerAccessScope != null) {
                claimsSet.claim("token", bearerAccessScope);
            }
            if (bearerAccessToken != null) {
                claimsSet.claim("token", bearerAccessToken);
            }

            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(ctx.jwk.getKeyID())
                    .build();

            var signedJwt = new SignedJWT(header, claimsSet.build());
            signedJwt.sign(createSigner(ctx.jwk));

            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign VP JWT", e);
        }
    }

    public String createVpJwt(String participantContextId, String audience) {
        var ctx = getParticipant(participantContextId);
        return createVpJwt(ctx, audience);
    }

    private String createVpJwt(ParticipantContext ctx, String audience) {
        try {
            var vpClaims = Map.of(
                    "@context", List.of("https://www.w3.org/2018/credentials/v1"),
                    "type", List.of("VerifiablePresentation"),
                    "verifiableCredential", List.copyOf(ctx.storedCredentials)
            );

            var now = Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(ctx.document.getId())
                    .subject(ctx.document.getId())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(now)
                    .notBeforeTime(now)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .claim("vp", vpClaims)
                    .audience(audience)
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(ctx.jwk.getKeyID())
                    .build();

            var signedJwt = new SignedJWT(header, claimsSet);
            signedJwt.sign(createSigner(ctx.jwk));

            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign VP JWT", e);
        }
    }


    private JWSSigner createSigner(JWK jwk) {
        try {
            if (jwk instanceof ECKey) {
                return new ECDSASigner((ECKey) jwk);
            }
            if (jwk instanceof OctetKeyPair) {
                return new Ed25519Signer((OctetKeyPair) jwk);
            }
            throw new IllegalArgumentException("Unsupported JWK type: " + jwk.getClass().getSimpleName());
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to create signer for JWK: " + jwk.getKeyID(), e);
        }
    }


    private static class ParticipantContext {
        private final DidDocument document;
        private final JWK jwk;
        private final List<String> storedCredentials = new ArrayList<>();

        ParticipantContext(DidDocument document, JWK jwk) {
            this.document = document;
            this.jwk = jwk;
        }
    }
}
