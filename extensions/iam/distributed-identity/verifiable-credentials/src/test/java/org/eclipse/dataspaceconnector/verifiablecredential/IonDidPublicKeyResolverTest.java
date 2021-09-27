package org.eclipse.dataspaceconnector.verifiablecredential;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.VerificationMethod;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.eclipse.dataspaceconnector.verifiablecredential.TestHelper.readFile;

class IonDidPublicKeyResolverTest {

    private static final String DID_URL = "did:ion:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA";
    private DidDocument didDocument;
    private IonDidPublicKeyResolver resolver;
    private IonClient ionClient;

    @BeforeEach
    void setUp() throws JOSEException {
        ionClient = niceMock(IonClient.class);
        resolver = new IonDidPublicKeyResolver(ionClient);
        var eckey = (ECKey) ECKey.parseFromPEMEncodedObjects(readFile("public_secp256k1.pem"));

        var publicKey = new EllipticCurvePublicKey(eckey.getCurve().getName(), eckey.getKeyType().getValue(), eckey.getX().toString(), eckey.getY().toString());


        didDocument = DidDocument.Builder.newInstance()
                .verificationMethod("#my-key1", DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019, publicKey)
                .service(Collections.singletonList(new Service("#my-service1", DidConstants.HUB_URL, "http://doesnotexi.st")))
                .build();
    }

    @Test
    void resolve() {
        expect(ionClient.resolve(DID_URL)).andReturn(didDocument);
        replay(ionClient);

        var pk = resolver.resolvePublicKey(DID_URL);
        assertThat(pk).isNotNull();
    }

    @Test
    void resolve_didNotFound() {
        expect(ionClient.resolve(DID_URL)).andReturn(null);
        replay(ionClient);

        var pk = resolver.resolvePublicKey(DID_URL);
        assertThat(pk).isNull();
    }

    @Test
    void resolve_didDoesNotContainPublicKey() {
        didDocument.getVerificationMethod().clear();
        expect(ionClient.resolve(DID_URL)).andReturn(didDocument);
        replay(ionClient);

        assertThatThrownBy(() -> resolver.resolvePublicKey(DID_URL)).isInstanceOf(PublicKeyResolutionException.class).hasMessage("DID does not contain a Public Key!");
    }

    @Test
    void resolve_didContainsMultipleKeys() throws JOSEException {
        var publicKey = (ECKey) ECKey.parseFromPEMEncodedObjects(readFile("public_secp256k1.pem"));
        var vm = VerificationMethod.Builder.create().id("second-key").type(DidConstants.JSON_WEB_KEY_2020).controller("")
                .publicKeyJwk(new EllipticCurvePublicKey(publicKey.getCurve().getName(), publicKey.getKeyType().getValue(), publicKey.getX().toString(), publicKey.getY().toString()))
                .build();
        didDocument.getVerificationMethod().add(vm);
        expect(ionClient.resolve(DID_URL)).andReturn(didDocument);
        replay(ionClient);

        assertThatThrownBy(() -> resolver.resolvePublicKey(DID_URL)).isInstanceOf(PublicKeyResolutionException.class).hasMessage("DID contains more than one \"Allowed Verification Type\"!");
    }

    @Test
    void resolve_publicKeyNotInPemFormat() {
        didDocument.getVerificationMethod().clear();
        var vm = VerificationMethod.Builder.create().id("second-key").type(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019).controller("")
                .publicKeyJwk(new EllipticCurvePublicKey("invalidCurve", "EC", null, null))
                .build();
        didDocument.getVerificationMethod().add(vm);

        expect(ionClient.resolve(DID_URL)).andReturn(didDocument);
        replay(ionClient);

        assertThatThrownBy(() -> resolver.resolvePublicKey(DID_URL)).isInstanceOf(PublicKeyResolutionException.class).hasMessageStartingWith("Public Key was not a valid EC Key!");
    }
}