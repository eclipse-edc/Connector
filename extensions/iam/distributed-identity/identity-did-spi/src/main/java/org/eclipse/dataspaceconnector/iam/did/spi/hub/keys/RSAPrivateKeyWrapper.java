package org.eclipse.dataspaceconnector.iam.did.spi.hub.keys;

import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSASigner;

import java.security.interfaces.RSAPrivateKey;

public class RSAPrivateKeyWrapper implements PrivateKeyWrapper {
    private final RSAPrivateKey privateKey;

    public RSAPrivateKeyWrapper(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public JWEDecrypter decrypter() {
        return new RSADecrypter(privateKey);
    }

    @Override
    public JWSSigner signer() {
        return new RSASSASigner(privateKey);
    }
}
