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

package org.eclipse.edc.iam.decentralizedclaims.spi.issuerservice;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test fixture that simulates a Verifiable Credential issuer with a resolvable {@code did:web} DID.
 * <p>
 * The issuer generates an EC key pair (P-256), hosts a DID document via WireMock, and can sign
 * verifiable credentials as JWTs.
 */
public class IssuerService {

    private final DidDocument didDocument;
    private final String did;
    private ECKey ecKey;

    public IssuerService(Integer port) {
        this(null, port);
    }

    public IssuerService(ECKey key, Integer port) {
        ecKey = key;
        did = "did:web:localhost%3A" + port + ":issuer";
        if (ecKey == null) {
            ecKey = generateEcKey(did);
        }
        didDocument = DidDocument.Builder.newInstance()
                .id(did)
                .verificationMethod(List.of(
                        VerificationMethod.Builder.newInstance()
                                .id(ecKey.getKeyID())
                                .type("JsonWebKey2020")
                                .controller(did)
                                .publicKeyJwk(ecKey.toPublicJWK().toJSONObject())
                                .build()
                ))
                .build();
    }

    private ECKey generateEcKey(String did) {
        try {
            return new ECKeyGenerator(Curve.P_256)
                    .keyID(did + "#key1")
                    .generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }


    public DidDocument getDidDocument() {
        return didDocument;
    }

    /**
     * Returns the {@code did:web} DID of this issuer.
     */
    public String getDid() {
        return did;
    }


    /**
     * Issues a verifiable credential as a signed JWT.
     *
     * @param subjectDid     the DID of the credential subject
     * @param credentialType the type of credential (e.g., "MembershipCredential")
     * @param subjectClaims  additional claims to include in the credential subject
     * @return the serialized JWT string representing the signed VC
     */
    public String issueCredential(String subjectDid, String credentialType, Map<String, Object> subjectClaims) {
        var credentialSubject = new HashMap<>(subjectClaims);
        credentialSubject.put("id", subjectDid);

        var vcClaims = Map.of(
                "@context", List.of("https://www.w3.org/2018/credentials/v1"),
                "type", List.of("VerifiableCredential", credentialType),
                "issuer", did,
                "issuanceDate", Instant.now().toString(),
                "credentialSubject", credentialSubject
        );

        return issueCredential(vcClaims);
    }

    /**
     * Issues a verifiable credential as a signed JWT with the given VC claims map.
     *
     * @param vcClaimsMap the value of the {@code vc} claim in the JWT
     * @return the serialized JWT string representing the signed VC
     */
    public String issueCredential(Map<String, Object> vcClaimsMap) {
        try {
            var now = Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(did)
                    .subject(did)
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(now)
                    .notBeforeTime(now)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .claim("vc", vcClaimsMap)
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(ecKey.getKeyID())
                    .build();

            var signedJwt = new SignedJWT(header, claimsSet);
            signedJwt.sign(new ECDSASigner(ecKey));

            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign VC JWT", e);
        }
    }
}
