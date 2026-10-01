/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.LinkedDataSignature;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.ld.signature.key.KeyPair;
import com.apicatalog.multibase.Multibase;
import com.apicatalog.vc.ModelVersion;
import com.apicatalog.vc.VcVocab;
import com.apicatalog.vc.Verifiable;
import com.apicatalog.vc.issuer.AbstractIssuer;
import com.apicatalog.vc.issuer.ProofDraft;
import com.apicatalog.vc.processor.ExpandedVerifiable;
import com.apicatalog.vc.proof.EmbeddedProof;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * replacement for the {@link com.apicatalog.vc.solid.SolidIssuer}, which would add a hardcoded {@code proofValue}, but
 * JsonWebSignature2020 needs a {@code jws} field.
 */
class JwsIssuer extends AbstractIssuer {
    JwsIssuer(Jws2020SignatureSuite jws2020SignatureSuite, KeyPair keyPair) {
        super(jws2020SignatureSuite, keyPair, Multibase.BASE_64_URL);
    }

    @Override
    protected ExpandedVerifiable sign(final ModelVersion version, final JsonArray context, final JsonObject expanded,
                                      final ProofDraft draft, final DocumentLoader loader) throws SigningError, DocumentError {

        if (keyPair.privateKey() == null || keyPair.privateKey().length == 0) {
            throw new SigningError(SigningError.Code.Internal,
                    new IllegalArgumentException("The private key is not provided, is null or an empty array."));
        }

        var verifiable = Verifiable.of(version, expanded);
        if (verifiable.isCredential() && verifiable.asCredential().isExpired()) {
            throw new SigningError(SigningError.Code.Expired);
        }

        verifiable.validate();

        var object = addIssuanceDateIfMissing(expanded, verifiable);

        var unsigned = EmbeddedProof.removeProofs(object);

        var signature = sign(context, unsigned, draft);

        var proofValue = Json.createValue(new String(signature));

        var signedProof = Jws2020ProofDraft.signed(draft.unsigned(), proofValue);

        return new ExpandedVerifiable(EmbeddedProof.addProof(object, signedProof), context, loader);
    }

    @Override
    protected byte[] sign(JsonArray context, JsonObject document, ProofDraft draft) throws SigningError {
        var unsignedDraft = draft.unsigned();

        var ldSignature = new LinkedDataSignature(draft.cryptoSuite());

        return ldSignature.sign(document, keyPair.privateKey(), unsignedDraft);
    }

    private JsonObject addIssuanceDateIfMissing(JsonObject object, Verifiable verifiable) {
        if (issuanceDateIsMissing(verifiable)) {

            var issuanceDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);

            return Json.createObjectBuilder(object)
                    .add(VcVocab.ISSUANCE_DATE.uri(), issuanceDate.toString())
                    .build();
        }

        return object;
    }

    private static boolean issuanceDateIsMissing(Verifiable verifiable) {
        return verifiable.isCredential() &&
                (verifiable.version() == null || ModelVersion.V11.equals(verifiable.version())) &&
                verifiable.asCredential().issuanceDate() == null;
    }
}
