package org.eclipse.dataspaceconnector.verifiablecredential;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.spi.security.KeyParser;

public class EcPrivateKeyPemParser implements KeyParser<ECKey> {

    @Override
    public boolean canParse(Class<?> aClass) {
        return aClass.equals(ECKey.class);
    }

    @Override
    public ECKey parse(String encodedContent) {
        try {
            return (ECKey) ECKey.parseFromPEMEncodedObjects(encodedContent);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }
}
