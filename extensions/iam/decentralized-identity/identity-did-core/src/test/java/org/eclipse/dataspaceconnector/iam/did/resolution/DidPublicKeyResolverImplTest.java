package org.eclipse.dataspaceconnector.iam.did.resolution;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Objects;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DidPublicKeyResolverImplTest {

    private static final String DID_URL = "did:ion:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA";
    private DidDocument didDocument;
    private DidPublicKeyResolverImpl resolver;
    private DidResolverRegistry resolverRegistry;

    @BeforeEach
    void setUp() throws JOSEException {
        resolverRegistry = mock(DidResolverRegistry.class);
        resolver = new DidPublicKeyResolverImpl(resolverRegistry);
        var eckey = (ECKey) ECKey.parseFromPEMEncodedObjects(readFile("public_secp256k1.pem"));

        var publicKey = new EllipticCurvePublicKey(eckey.getCurve().getName(), eckey.getKeyType().getValue(), eckey.getX().toString(), eckey.getY().toString());

        didDocument = DidDocument.Builder.newInstance()
                .verificationMethod("#my-key1", DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019, publicKey)
                .service(Collections.singletonList(new Service("#my-service1", DidConstants.HUB_URL, "http://doesnotexi.st")))
                .build();
    }

    @Test
    void resolve() {
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolvePublicKey(DID_URL);

        assertThat(result.getContent()).isNotNull();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didNotFound() {
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.failure("Not found"));

        var result = resolver.resolvePublicKey(DID_URL);

        assertThat(result.failed()).isTrue();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didDoesNotContainPublicKey() {
        didDocument.getVerificationMethod().clear();
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolvePublicKey(DID_URL);

        assertThat(result.failed()).isTrue();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_didContainsMultipleKeys() throws JOSEException {
        var publicKey = (ECKey) ECKey.parseFromPEMEncodedObjects(readFile("public_secp256k1.pem"));
        var vm = VerificationMethod.Builder.create().id("second-key").type(DidConstants.JSON_WEB_KEY_2020).controller("")
                .publicKeyJwk(new EllipticCurvePublicKey(publicKey.getCurve().getName(), publicKey.getKeyType().getValue(), publicKey.getX().toString(), publicKey.getY().toString()))
                .build();
        didDocument.getVerificationMethod().add(vm);
        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolvePublicKey(DID_URL);

        assertThat(result.failed()).isTrue();
        verify(resolverRegistry).resolve(DID_URL);
    }

    @Test
    void resolve_publicKeyNotInPemFormat() {
        didDocument.getVerificationMethod().clear();
        var vm = VerificationMethod.Builder.create().id("second-key").type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019).controller("")
                .publicKeyJwk(new EllipticCurvePublicKey("invalidCurve", "EC", null, null))
                .build();
        didDocument.getVerificationMethod().add(vm);

        when(resolverRegistry.resolve(DID_URL)).thenReturn(Result.success(didDocument));

        var result = resolver.resolvePublicKey(DID_URL);

        assertThat(result.failed()).isTrue();
        verify(resolverRegistry).resolve(DID_URL);
    }

    public String readFile(String filename) {
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        Scanner s = new Scanner(Objects.requireNonNull(stream)).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
