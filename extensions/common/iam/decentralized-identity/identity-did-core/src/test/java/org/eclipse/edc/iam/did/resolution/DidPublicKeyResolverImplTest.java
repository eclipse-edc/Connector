/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.iam.did.resolution;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DidPublicKeyResolverImplTest {

    public static final String KEYID = "#my-key1";
    private static final String DID_URL = "did:web:example.com";
    private final DidResolverRegistry resolverRegistry = mock();
    private final KeyParserRegistry keyParserRegistry = mock();
    private final DidPublicKeyResolverImpl resolver = new DidPublicKeyResolverImpl(keyParserRegistry, resolverRegistry);

    public static String readFile(String filename) throws IOException {
        try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            var s = new Scanner(Objects.requireNonNull(is)).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    @BeforeEach
    public void setUp() throws JOSEException {

        when(keyParserRegistry.parse(anyString())).thenReturn(Result.success(new ECKeyGenerator(Curve.P_256).generate().toPublicKey()));
    }

    private DidDocument createDidDocument(String verificationMethodId) {
        return createDidDocumentBuilder(verificationMethodId).build();
    }

    private DidDocument createDidDocument() {
        return createDidDocumentBuilder(KEYID).build();
    }

    private VerificationMethod createVerificationMethod(String verificationMethodId, ECKey eckey) {
        return VerificationMethod.Builder.newInstance()
                .id(verificationMethodId)
                .type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019)
                .publicKeyJwk(eckey.toPublicJWK().toJSONObject())
                .build();
    }

    private VerificationMethod createVerificationMethod(String verificationMethodId) {
        try {
            var eckey = (ECKey) ECKey.parseFromPEMEncodedObjects(readFile("public_secp256k1.pem"));
            return createVerificationMethod(verificationMethodId, eckey);
        } catch (JOSEException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private DidDocument.Builder createDidDocumentBuilder(String verificationMethodId) {
        var vm = createVerificationMethod(verificationMethodId);
        return DidDocument.Builder.newInstance()
                .verificationMethod(List.of(vm))
                .service(Collections.singletonList(new Service("#my-service1", "MyService", "http://doesnotexi.st")));
    }

    @Test
    void resolve() {
        var didDocument = createDidDocument();
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL + KEYID);

        assertThat(result).isSucceeded().isNotNull();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_withVerificationMethodUrlAsId() throws IOException, JOSEException {
        var didDocument = createDidDocument(DID_URL + KEYID);
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL + KEYID);

        assertThat(result).isSucceeded().isNotNull();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didNotFound() {
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.failure("Not found"));

        var result = resolver.resolveKey(DID_URL + KEYID);

        assertThat(result).isFailed();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didDoesNotContainPublicKey() {
        var didDocument = createDidDocument();
        didDocument.getVerificationMethod().clear();
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL + KEYID);

        assertThat(result).isFailed();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didContainsMultipleKeysWithSameKeyId() throws JOSEException, IOException {
        var vm = createVerificationMethod(KEYID);
        var vm1 = createVerificationMethod(KEYID);
        var didDocument = createDidDocumentBuilder(KEYID).verificationMethod(List.of(vm, vm1)).build();
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL + KEYID);

        assertThat(result).isFailed()
                .detail().contains("Every verification method must have a unique ID");
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didContainsMultipleKeysWithSameKeyId_withRelativeAndFullUrl() {
        var vm = createVerificationMethod(DID_URL + KEYID);
        var vm1 = createVerificationMethod(KEYID);

        var didDocument = createDidDocumentBuilder(KEYID).verificationMethod(List.of(vm, vm1)).build();
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL + KEYID);

        assertThat(result).isFailed()
                .detail().contains("Every verification method must have a unique ID");
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_publicKeyNotInPemFormat() {
        var secondKeyId = "#second-key";
        var vm = VerificationMethod.Builder.newInstance().id(secondKeyId).type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019).controller("")
                .publicKeyJwk(Map.of("kty", "EC"))
                .build();
        var vm1 = createVerificationMethod(KEYID);

        var didDocument = createDidDocumentBuilder(KEYID).verificationMethod(List.of(vm, vm1)).build();

        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL + secondKeyId);

        assertThat(result).isFailed();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_keyIdNullMultipleKeys() throws JOSEException, IOException {
        var didDocument = createDidDocument();
        var publicKey = (ECKey) ECKey.parseFromPEMEncodedObjects(readFile("public_secp256k1.pem"));
        var vm = VerificationMethod.Builder.newInstance().id("#my-key2").type(DidConstants.JSON_WEB_KEY_2020).controller("")
                .publicKeyJwk(publicKey.toJSONObject())
                .build();
        didDocument.getVerificationMethod().add(vm);
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL);
        assertThat(result).isFailed()
                .detail().contains("The key ID ('kid') is mandatory if DID contains >1 verification methods.");
    }

    @Test
    void resolve_keyIdIsNull_onlyOneVerificationMethod() {
        var didDocument = createDidDocument();
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolveKey(DID_URL);

        assertThat(result).isSucceeded().isNotNull();
        verify(resolverRegistry).resolve(DID_URL);
    }
}
