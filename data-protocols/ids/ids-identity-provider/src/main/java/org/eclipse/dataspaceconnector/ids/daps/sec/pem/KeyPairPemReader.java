package org.eclipse.dataspaceconnector.ids.daps.sec.pem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class KeyPairPemReader extends PemReader {
    private static final String ALGORITHM = "RSA";
    private static final Provider PROVIDER = new BouncyCastleProvider();

    static {
        java.security.Security.addProvider(PROVIDER);
    }

    private final KeyFactory keyFactory;

    public KeyPairPemReader() throws NoSuchAlgorithmException {
        keyFactory = KeyFactory.getInstance(ALGORITHM);
    }

    private RSAPrivateCrtKey readPrivateKey(final InputStream inputStream, final char[] passphrase) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        final PemObject pemObject = read(inputStream);
        final byte[] content = pemObject.getContent();

        EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(content);
        if (passphrase != null && passphrase.length > 0) {
            // Note that OID 1.2.840.113549.1.5.13 is neither by Sun nor by BC supported
            final EncryptedPrivateKeyInfo pkInfo = new EncryptedPrivateKeyInfo(content);
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase);
            final SecretKeyFactory pbeKeyFactory = SecretKeyFactory.getInstance(pkInfo.getAlgName());
            keySpec = pkInfo.getKeySpec(pbeKeyFactory.generateSecret(pbeKeySpec));
        }

        return (RSAPrivateCrtKey) keyFactory.generatePrivate(keySpec);
    }

    public KeyPair readKeyPair(final InputStream inputStream) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        return readKeyPair(inputStream, null);
    }

    public KeyPair readKeyPair(final InputStream inputStream, final char[] passphrase) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        final RSAPrivateCrtKey privateKey = readPrivateKey(inputStream, passphrase);

        final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());

        final PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        return new KeyPair(publicKey, privateKey);
    }
}
