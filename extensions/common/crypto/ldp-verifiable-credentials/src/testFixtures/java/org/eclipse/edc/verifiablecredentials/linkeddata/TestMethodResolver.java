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

package org.eclipse.edc.verifiablecredentials.linkeddata;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.method.resolver.MethodResolver;
import com.apicatalog.vc.proof.Proof;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.security.signature.jws2020.JsonWebKeyPair;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link MethodResolver} that resolves verification methods from an in-memory map, standing in for a DID document.
 */
public class TestMethodResolver implements MethodResolver {
    private static final URI JSON_WEB_KEY_2020 = URI.create("https://w3id.org/security#JsonWebKey2020");
    private final Map<URI, VerificationMethod> methods = new HashMap<>();

    /**
     * Publishes the public part of the key under the given verification method ID and returns a reference to it, i.e. a
     * verification method without key material, for use in a proof.
     */
    public VerificationMethod publish(String id, JWK key) {
        var methodId = URI.create(id);
        methods.put(methodId, new JsonWebKeyPair(methodId, JSON_WEB_KEY_2020, null, key.toPublicJWK()));
        return new JsonWebKeyPair(methodId, null, null, null);
    }

    /**
     * Makes the resolver return the given method when the given ID is resolved.
     */
    public void register(String id, VerificationMethod method) {
        methods.put(URI.create(id), method);
    }

    @Override
    public VerificationMethod resolve(URI id, DocumentLoader loader, Proof proof) throws DocumentError {
        var method = methods.get(id);
        if (method == null) {
            throw new DocumentError(DocumentError.ErrorType.Unknown, "ProofVerificationMethod");
        }
        return method;
    }

    @Override
    public boolean isAccepted(URI id) {
        return true;
    }
}
