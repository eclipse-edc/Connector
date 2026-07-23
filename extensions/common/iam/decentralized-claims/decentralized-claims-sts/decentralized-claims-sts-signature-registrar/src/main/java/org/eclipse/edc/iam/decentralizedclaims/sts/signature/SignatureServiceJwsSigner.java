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

package org.eclipse.edc.iam.decentralizedclaims.sts.signature;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import org.eclipse.edc.spi.security.SignatureService;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * A Nimbus {@link JWSSigner} that delegates the actual signing operation to a {@link SignatureService}. The private key
 * never leaves the signing service: the signing input is transmitted to the service, signed there with the configured
 * key, and only the signature is returned.
 * <p>
 * Some {@link SignatureService} implementations (notably the Hashicorp Vault transit engine) return the signature
 * wrapped as {@code vault:<key-version>:<base64-signature>}. This signer transparently unwraps that form when present;
 * otherwise it treats the returned bytes as the raw signature, as per the generic {@link SignatureService} contract.
 * <p>
 * Note: only the EdDSA algorithm (Ed25519 curve) is supported for now, for which the signature is a raw 64-byte
 * {@code R||S} value, which is exactly what JWS expects.
 */
public class SignatureServiceJwsSigner implements JWSSigner {

    private static final String VAULT_SIGNATURE_PATTERN = "vault:v\\d+:.*";

    private final SignatureService signatureService;
    private final String participantContextId;
    private final String keyName;
    private final JCAContext jcaContext = new JCAContext();

    /**
     * Creates a signer bound to a single signing key, scoped to a participant context (vault partition).
     *
     * @param signatureService     the signature service that performs the signing.
     * @param participantContextId the participant context id used as the vault partition. May be null, indicating the default partition.
     * @param keyName              the name/identifier of the key to sign with.
     */
    public SignatureServiceJwsSigner(SignatureService signatureService, @Nullable String participantContextId, String keyName) {
        this.signatureService = signatureService;
        this.participantContextId = participantContextId;
        this.keyName = keyName;
    }

    @Override
    public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
        var result = signatureService.sign(participantContextId, keyName, signingInput, header.getAlgorithm().getName());
        if (result.failed()) {
            throw new JOSEException("Failed to sign with key '%s': %s".formatted(keyName, result.getFailureDetail()));
        }

        var content = result.getContent();
        // some implementations (e.g. Hashicorp Vault) wrap the signature as "vault:<key-version>:<base64-signature>"
        var asString = new String(content, StandardCharsets.UTF_8);
        var rawSignature = asString.matches(VAULT_SIGNATURE_PATTERN)
                ? Base64.getDecoder().decode(asString.substring(asString.lastIndexOf(':') + 1))
                : content;
        return Base64URL.encode(rawSignature);
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return Set.of(JWSAlgorithm.EdDSA);
    }

    @Override
    public JCAContext getJCAContext() {
        return jcaContext;
    }
}
