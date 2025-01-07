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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.jsonld.loader.SchemeRouter;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.VerificationError;
import com.apicatalog.vc.verifier.Verifier;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.readResourceAsJson;

class VerifierTests {

    private final Jws2020SignatureSuite jws2020suite = new Jws2020SignatureSuite(JacksonJsonLd.createObjectMapper());
    private final TestDocumentLoader loader = new TestDocumentLoader("https://org.eclipse.edc/", "jws2020/verifying/", SchemeRouter.defaultInstance());
    private final Verifier verifier = Verifier.with(jws2020suite).loader(loader);

    @DisplayName("t0001: valid signed VC")
    @Test
    void verifyValidVc() throws VerificationError, DocumentError {
        var vc = readResourceAsJson("jws2020/verifying/0001_vc.json");

        var result = verifier.verify(vc);
        assertThatNoException().isThrownBy(result::validate);
    }

    @DisplayName("t0002: forged credentials subject")
    @Test
    void verify_forgedSubject() {
        var vc = readResourceAsJson("jws2020/verifying/0002_vc_forged.json");
        assertThatThrownBy(() -> verifier.verify(vc)).isInstanceOf(VerificationError.class);
    }

    @DisplayName("t0003: valid VC with embedded verification method")
    @Test
    void verifyVc_withEmbeddedVerificationMethod() throws VerificationError, DocumentError {
        var vc = readResourceAsJson("jws2020/verifying/0003_vc_embedded.json");
        var result = verifier.verify(vc);
        assertThatNoException().isThrownBy(result::validate);
    }

    @DisplayName("t0004: proof set of two valid proofs")
    @Test
    void verify_multipleValidProofs() throws VerificationError, DocumentError {
        var vc = readResourceAsJson("jws2020/verifying/0004_vc_two_valid_proofs.json");
        var result = verifier.verify(vc);
        assertThatNoException().isThrownBy(result::validate);
    }

    @DisplayName("t0005: proof set having one forged proof")
    @Test
    void verify_oneForgedProof() {
        var vc = readResourceAsJson("jws2020/verifying/0005_vc_one_forged_proof.json");
        assertThatThrownBy(() -> verifier.verify(vc)).isInstanceOf(VerificationError.class);
    }

    /**
     * The did:key method is not yet supported, since it is only a community draft, and implementability in conjunction with JWS2020 is
     * <a href="https://w3c-ccg.github.io/did-method-key/#create">unclear</a>. Furthermore, the "did:key" implementation relies
     * on <a href="https://github.com/multiformats/java-multibase">this Multibase library</a>, which is not available from MavenCentral.
     * <p>
     * The biggest challenge with Jws will be to reconstruct the key type/curve from just the public key.
     */
    @Disabled("did:key is not supported")
    @DisplayName("t0006: DID key as verification method (not yet supported)")
    @Test
    void verify_didKeyAsVerificationMethod() {
        var vc = readResourceAsJson("jws2020/verifying/0006_vc_did_key.json");
        assertThatThrownBy(() -> verifier.verify(vc)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Cannot deserialize public key, expected JWK format");
    }

    @DisplayName("t0007: valid signed VP")
    @Test
    void verify_validSignedVp() throws VerificationError, DocumentError {
        var vc = readResourceAsJson("jws2020/verifying/0007_vp_compacted.json");
        var result = verifier.verify(vc);
        assertThatNoException().isThrownBy(result::validate);
    }

    @DisplayName("t0008: forged signed VP")
    @Test
    void verify_forgedSignedVp() {
        var vc = readResourceAsJson("jws2020/verifying/0007_vp_compacted_forged.json");
        assertThatThrownBy(() -> verifier.verify(vc)).isInstanceOf(VerificationError.class);
    }
}
