package org.eclipse.dataspaceconnector.iam.ion.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.ion.IonException;

import java.io.IOException;

public class JsonCanonicalizer {
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public static byte[] canonicalizeAsBytes(Object object) {
        try {
            String json = mapper.writeValueAsString(object);
            var jc = new org.erdtman.jcs.JsonCanonicalizer(json);

            return jc.getEncodedUTF8();
        } catch (IOException e) {
            throw new IonException(e);
        }

    }

    public static String canonicalizeAsString(Object object) {
        try {
            String json = mapper.writeValueAsString(object);
            var jc = new org.erdtman.jcs.JsonCanonicalizer(json);
            return jc.getEncodedString();
        } catch (IOException e) {
            throw new IonException(e);
        }
    }
}
