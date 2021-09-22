package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import org.eclipse.dataspaceconnector.spi.EdcException;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.ECDH_ES_A256KW;

/**
 * Writes a JWE containing a typed payload.
 */
public class GenericJweWriter extends AbstractJweWriter<GenericJweWriter> {
    private Object payload;

    @Override
    public String buildJwe() {
        try {
            var jwePayload = new Payload(objectMapper.writeValueAsString(payload));
            var jweHeader = new JWEHeader.Builder(ECDH_ES_A256KW, A256GCM).build();
            var jweObject = new JWEObject(jweHeader, jwePayload);
            jweObject.encrypt(publicKey.encrypter());
            return jweObject.serialize();
        } catch (JsonProcessingException | JOSEException e) {
            throw new EdcException(e);
        }
    }


    public GenericJweWriter payload(Object payload) {
        this.payload = payload;
        return this;
    }
}
