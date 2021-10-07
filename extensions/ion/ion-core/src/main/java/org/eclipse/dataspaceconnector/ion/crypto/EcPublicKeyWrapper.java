package org.eclipse.dataspaceconnector.ion.crypto;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.ion.IonCryptoException;

public class EcPublicKeyWrapper implements PublicKeyWrapper {
    private final ECKey publicKey;

    public EcPublicKeyWrapper(ECKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public JWEEncrypter encrypter() {
        try {
            return new ECDHEncrypter(publicKey);
        } catch (JOSEException e) {
            throw new IonCryptoException(e);
        }
    }

    @Override
    public JWSVerifier verifier() {
        try {
            return new ECDSAVerifier(publicKey);
        } catch (JOSEException e) {
            throw new IonCryptoException(e);
        }
    }
}
