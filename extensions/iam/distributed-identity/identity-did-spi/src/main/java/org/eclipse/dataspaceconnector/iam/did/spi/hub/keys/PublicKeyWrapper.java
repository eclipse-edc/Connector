package org.eclipse.dataspaceconnector.iam.did.spi.hub.keys;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;

public interface PublicKeyWrapper {

    JWEEncrypter encrypter();

    JWSVerifier verifier();

    default JWEAlgorithm jweAlgorithm() {
        return JWEAlgorithm.ECDH_ES_A256KW;
    }

    default EncryptionMethod encryptionMethod() {
        return EncryptionMethod.A256GCM;
    }
}
