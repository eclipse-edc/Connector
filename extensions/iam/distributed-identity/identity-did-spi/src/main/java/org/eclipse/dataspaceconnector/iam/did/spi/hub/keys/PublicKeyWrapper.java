package org.eclipse.dataspaceconnector.iam.did.spi.hub.keys;

import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;

public interface PublicKeyWrapper {

    JWEEncrypter encrypter();

    JWSVerifier verifier();
}
