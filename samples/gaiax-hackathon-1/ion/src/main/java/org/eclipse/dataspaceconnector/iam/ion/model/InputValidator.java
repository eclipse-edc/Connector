package org.eclipse.dataspaceconnector.iam.ion.model;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.ion.IonCryptoException;

import java.util.HashSet;
import java.util.Set;

public class InputValidator {
    public static void validateEs256kOperationKey(JWK key, String visibility) {
        var allowedProperties = new HashSet<>(Set.of("kty", "crv", "x", "y"));
        if (visibility.equals("private")) {
            allowedProperties.add("d");
        }

        if (!(key instanceof ECKey)) {
            throw new IonCryptoException("Can only support EC keys for now, but an object of type " + key.getClass() + "was found!");
        }
        var ecKey = (ECKey) key;

        if (!ecKey.getCurve().getName().equals("secp256k1")) {
            throw new IonCryptoException("Only secp256k1 is supported at the moment, but " + (ecKey).getCurve().getName() + " was supplied!");
        }

        if (!ecKey.getKeyType().getValue().equals("EC")) {
            throw new IonCryptoException("Only EC keys are supported at the moment, but " + key.getKeyType().getValue() + " was supplied!");
        }

        int xLen = ecKey.getX().toString().length();
        if (xLen != 43) {
            throw new IonCryptoException("SECP256k1 JWK 'x' property must be 43 bytes but found " + xLen);
        }

        int yLen = ecKey.getY().toString().length();
        if (yLen != 43) {
            throw new IonCryptoException("SECP256k1 JWK 'y' property must be 43 bytes but found " + yLen);
        }

        if (visibility.equals("private") && (ecKey.getD() == null || ecKey.getD().toString().length() != 43)) {
            throw new IonCryptoException("SECP256k1 JWK 'd' property must be 43 bytes");
        }

    }
}
