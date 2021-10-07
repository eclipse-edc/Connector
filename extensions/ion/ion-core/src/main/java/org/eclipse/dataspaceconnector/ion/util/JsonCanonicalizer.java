package org.eclipse.dataspaceconnector.ion.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.ion.IonException;

import java.io.IOException;

/**
 * Generates "canonical" JSON according to JCS (https://tools.ietf.org/id/draft-rundgren-json-canonicalization-scheme-05.html)
 */
public class JsonCanonicalizer {
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public static byte[] canonicalizeAsBytes(Object object) {
        try {
            String json = MAPPER.writeValueAsString(object);
            var jc = new org.erdtman.jcs.JsonCanonicalizer(json);

            return jc.getEncodedUTF8();
        } catch (IOException e) {
            throw new IonException(e);
        }

    }

    public static String canonicalizeAsString(Object object) {
        try {
            String json = MAPPER.writeValueAsString(object);
            var jc = new org.erdtman.jcs.JsonCanonicalizer(json);
            return jc.getEncodedString();
        } catch (IOException e) {
            throw new IonException(e);
        }
    }
}
