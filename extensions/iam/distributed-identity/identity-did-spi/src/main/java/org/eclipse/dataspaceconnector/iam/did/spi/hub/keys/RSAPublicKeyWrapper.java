package org.eclipse.dataspaceconnector.iam.did.spi.hub.keys;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;

import java.security.interfaces.RSAPublicKey;

public class RSAPublicKeyWrapper implements PublicKeyWrapper {
    private final RSAPublicKey publicKey;

    public RSAPublicKeyWrapper(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public JWEEncrypter encrypter() {
        return new RSAEncrypter(publicKey);
    }

    @Override
    public JWSVerifier verifier() {
        return new RSASSAVerifier(publicKey);
    }

    @Override
    public JWEAlgorithm jweAlgorithm() {
        return JWEAlgorithm.RSA_OAEP_256;
    }
}
