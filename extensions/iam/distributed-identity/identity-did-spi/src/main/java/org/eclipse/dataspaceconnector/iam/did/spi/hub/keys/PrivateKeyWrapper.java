package org.eclipse.dataspaceconnector.iam.did.spi.hub.keys;

import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;

public interface PrivateKeyWrapper {

    JWEDecrypter decrypter();

    JWSSigner signer();
}
