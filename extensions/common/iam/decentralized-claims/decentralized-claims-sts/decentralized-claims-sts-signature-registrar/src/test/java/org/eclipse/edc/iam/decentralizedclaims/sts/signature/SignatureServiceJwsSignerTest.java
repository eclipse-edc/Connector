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
import com.nimbusds.jose.util.Base64URL;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.SignatureService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignatureServiceJwsSignerTest {

    private static final String KEY_NAME = "signing-key";
    private final SignatureService signatureService = mock();
    private final SignatureServiceJwsSigner signer = new SignatureServiceJwsSigner(signatureService, KEY_NAME);

    private static byte[] sampleSignature() {
        var rawSignature = new byte[64];
        for (var i = 0; i < rawSignature.length; i++) {
            rawSignature[i] = (byte) i;
        }
        return rawSignature;
    }

    @Test
    void supportedJwsAlgorithms_shouldOnlyBeEdDsa() {
        assertThat(signer.supportedJWSAlgorithms()).containsExactly(JWSAlgorithm.EdDSA);
    }

    @Test
    void sign_shouldStripVaultPrefix_whenPresent() throws JOSEException {
        var rawSignature = sampleSignature();
        var vaultResponse = ("vault:v1:" + Base64.getEncoder().encodeToString(rawSignature)).getBytes(StandardCharsets.UTF_8);
        when(signatureService.sign(eq(KEY_NAME), any(), any())).thenReturn(Result.success(vaultResponse));

        var result = signer.sign(new JWSHeader(JWSAlgorithm.EdDSA), "signing-input".getBytes(StandardCharsets.UTF_8));

        assertThat(result).isEqualTo(Base64URL.encode(rawSignature));
    }

    @Test
    void sign_shouldUseRawBytes_whenNoVaultPrefix() throws JOSEException {
        var rawSignature = sampleSignature();
        when(signatureService.sign(eq(KEY_NAME), any(), any())).thenReturn(Result.success(rawSignature));

        var result = signer.sign(new JWSHeader(JWSAlgorithm.EdDSA), "signing-input".getBytes(StandardCharsets.UTF_8));

        assertThat(result).isEqualTo(Base64URL.encode(rawSignature));
    }

    @Test
    void sign_shouldThrow_whenSignatureServiceFails() {
        when(signatureService.sign(eq(KEY_NAME), any(), any())).thenReturn(Result.failure("signing service down"));

        assertThatThrownBy(() -> signer.sign(new JWSHeader(JWSAlgorithm.EdDSA), "signing-input".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(JOSEException.class)
                .hasMessageContaining("signing service down");
    }
}
